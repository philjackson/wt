(ns wt.core
  (:require [ajax.core :refer [GET]]
            [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [cljsjs.lunrjs :as lunr]
            [secretary.core :as secretary :include-macros true]
            [cljs-time.core :as time]
            [cljs-time.format :as format]
            [cljs-time.core :as local]
            [accountant.core :as accountant]
            [wt.log :refer [log]]))

(def loaded-locales (atom {}))
(def current-time (atom (time/now)))

(def idx (.lunr js/window #(this-as this
                             (.ref this "id")
                             (.field this "city")
                             (.field this "continent"))))

(defn parse-tz-id [id]
  (zipmap [:continent :city]
          (clojure.string/split id "/")))

(defn local-date-time-format []
  (js->clj (.resolvedOptions (.DateTimeFormat js/Intl))))

(defn local-time []
  (time/local-date-time (time/now)))

(defn extract-current-tz [timestamps]
  (first (filter #(or (< (time/epoch) (first %))
                      (nil? (first %))) timestamps)))

(defn async-fetch-locale [name]
  (let [{:keys [continent city]} (parse-tz-id name)
        response (GET (str "/tz/" continent "/" city ".json")
                     {:response-format :json
                      :handler #(swap! loaded-locales assoc name (extract-current-tz %))})]))

(defn format-time [time fmt]
  (cond
    (= fmt :12) [:div.time
                 [:span.hours (format/unparse
                               (format/formatter "h")
                               time)]
                 [:span.meridiem (format/unparse
                                  (format/formatter "a")
                                  time)]]
    (= fmt :24) [:div.time
                 [:span.hours (format/unparse
                               (format/formatter "HH")
                               time)]]))

;; -------------------------
;; Views
(defn hour-strip [name]
  ;; before mount
  (async-fetch-locale name)

  ;; render
  (fn []
    (let [{:keys [continent city]} (parse-tz-id name)]
      [:tr
       [:td.name
        [:div.continent continent]
        [:div.city city]]
       (when-let [inf (get @loaded-locales name)]
         (let [[_, zonename, offset] inf
               start (time/minus @current-time (time/minutes offset))]
           (doall
            (for [i (range 0 24)]
              (let [next (time/plus start (time/hours i))
                    hour (time/hour next)]
                (with-meta
                  (cond
                    ;; represents a new day
                    (= hour 0) [:td.new-day
                                [:div.day (format/unparse
                                           (format/formatter "dd")
                                           next)]
                                [:div.month (format/unparse
                                             (format/formatter "MMM")
                                             next)]]

                    ;; represents the first hour (current hour in local time)
                    (= i 0) [:td.first-day
                             (format/unparse
                              (format/formatter "hh:mm")
                              next)]

                    ;; all other hours
                    :else [:td (format-time next :24)])
                  {:key i}))))))])))

(defn home-page []
  (let [now (time/now)]
    [:table.strips
     [:tbody
      [hour-strip (get (local-date-time-format) "timeZone")]
      [hour-strip "Europe/Paris"]
      [hour-strip "Africa/Abidjan"]
      [hour-strip "US/Eastern"]
      [hour-strip "Asia/Hong_Kong"]]]))

(defn about-page []
  [:div [:h2 "About wt"]
   [:div [:a {:href "/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

;; -------------------------
;; Initialize app

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
  (GET "/tz/index.json"
      {:response-format :json
       :handler load-index})
  (.setInterval js/window #(reset! current-time (time/now)) 30000)
  (mount-root))
