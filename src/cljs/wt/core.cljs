(ns wt.core
  (:require [ajax.core :refer [GET]]
            [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [cljsjs.lunrjs]
            [cljsjs.react-select]
            [secretary.core :as secretary :include-macros true]
            [alandipert.storage-atom :refer [local-storage]]
            [cljs-time.core :as time]
            [cljs-time.format :as format]
            [cljs-time.core :as local]
            [accountant.core :as accountant]
            [wt.log :refer [log]]))

(defonce options {:format (atom :12)})

(def loaded-locales (atom {}))
(def current-time (atom (time/now)))

;; setup with some test zones. The first one is the user's home location.
(def timezones-to-show (local-storage (atom #{"Europe/London"
                                              "Europe/Paris"
                                              "Europe/Rome"
                                              "Asia/Tokyo"
                                              "America/New_York"})
                                      :stimezones))

(defonce idx (.lunr js/window #(this-as this
                                 (.ref this "id")
                                 (.field this "city")
                                 (.field this "continent"))))

(defn parse-tz-id [id]
  (zipmap [:continent :city]
          (clojure.string/split id "/")))

(defn local-time []
  (time/local-date-time (time/now)))

(defn extract-current-tz [timestamps]
  (first (filter #(or (< (time/epoch) (first %))
                      (nil? (first %))) timestamps)))

(defn async-fetch-locale [name]
  (let [{:keys [continent city]} (parse-tz-id name)
        response (GET (str "tz/" continent "/" city ".json")
                     {:response-format :json
                      :handler #(swap! loaded-locales assoc name (extract-current-tz %))})]))

(defn format-time [time fmt offset]
  (let [stupid-offset (rem offset 60)]
    (cond
      (= fmt :12) [:div.time
                   [:div.hours (format/unparse
                                (format/formatter "h")
                                time)
                    (when-not (= stupid-offset 0)
                      [:span.extra-mins ":" stupid-offset])]
                   
                   [:div.meridiem (format/unparse
                                   (format/formatter "a")
                                   time)]]
      (= fmt :24) [:div.time
                   [:span.hours (format/unparse
                                 (format/formatter "H")
                                 time)]])))

;; -------------------------
;; Views
(defn hour-strip [name]
  ;; before mount
  (async-fetch-locale name)

  ;; render
  (fn []
    (let [{:keys [continent city]} (parse-tz-id name)]
      [:tr
       [:td.delete {:on-click (fn [] (swap! timezones-to-show #(remove (fn [n] (= n name)) %)))}
        "✖"]
       [:td.name
        [:div.city (clojure.string/replace city "_" " ")]
        [:div.continent continent]]
       (when-let [inf (get @loaded-locales name)]
         (let [[_, zonename, offset] inf
               start (time/minus @current-time (time/minutes offset))]
           (doall
            (for [i (range 0 24)]
              (let [next (time/plus start (time/hours i))
                    hour (time/hour next)]
                (with-meta
                  (cond
                    ;; represents the first hour (current hour in local time)
                    (= i 0)
                    [:td.first-day
                     [:div.time (format/unparse
                                 (format/formatter "HH:mm")
                                 next)]
                     [:div.date (format/unparse
                                 (format/formatter "dd MMM")
                                 next)]]

                    ;; represents a new day
                    (= hour 0)
                    [:td.new-day
                     [:div.day (format/unparse
                                (format/formatter "dd")
                                next)]
                     [:div.month (format/unparse
                                  (format/formatter "MMM")
                                  next)]]

                    (and (> hour 8) (< hour 19))
                    [:td.work-hour (format-time next @(:format options) offset)]

                    ;; all other hours
                    :else
                    [:td.hour (format-time next @(:format options) offset)])
                  {:key i}))))))])))

(defn search-box []
  [:> (.-Async js/Select) {:loadOptions (fn [input cb]
                                          (let [res (js->clj (.search idx input))
                                                options (mapv (fn [l]
                                                                {:value (get l "ref")
                                                                 :label (get l "ref")}) res)]
                                            (cb nil (clj->js {:options options
                                                              :complete (= (count options) 1)
                                                              :autoload false}))))
                           :placeholder "Name of a location..."
                           :onChange (fn [s] (swap! timezones-to-show conj (get (js->clj s) "value")))}])

(defn home-page []
  (let [now (time/now)]
    [:div
     [search-box]
     [:table.strips
      [:tbody
       (doall
        (for [z @timezones-to-show]
          ^{:key z} [hour-strip z]))]]]))

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

;; -------------------------
;; Initialise app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn load-index [docs]
  (doall
   (doseq [doc docs]
     (.add idx (clj->js doc)))))

(defn init! []
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (secretary/dispatch! path))
    :path-exists?
    (fn [path]
      (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (GET "tz/index.json"
      {:response-format :json
       :handler load-index})
  (.setInterval js/window #(reset! current-time (time/now)) 30000)
  (mount-root))
