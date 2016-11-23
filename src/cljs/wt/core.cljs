(ns wt.core
  (:require [ajax.core :refer [GET]]
            [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [cljs-time.core :as time]
            [cljs-time.format :as format]
            [cljs-time.core :as local]
            [accountant.core :as accountant]
            [wt.log :refer [log]]))

(def loaded-locales (atom {}))

(defn local-date-time-format []
  (js->clj (.resolvedOptions (.DateTimeFormat js/Intl))))

(defn local-time []
  (time/local-date-time (time/now)))

(defn extract-current-tz [timestamps]
  (first (filter #(or (< (time/epoch) (first %))
                      (nil? (first %))) timestamps)))

(defn async-fetch-locale [name]
  (let [[continent city] (clojure.string/split name "/")
        response (GET (str "/tz/" continent "/" city)
                     {:response-format :json
                      :handler #(swap! loaded-locales assoc name (extract-current-tz %))})]))

;; -------------------------
;; Views
(defn hour-strip [now name]
  ;; before mount
  (async-fetch-locale name)

  ;; render
  (fn []
    [:div.hour-columns
     [:div.name name]
     (when-let [inf (get @loaded-locales name)]
       (let [[_, zonename, offset] inf
             start (time/minus now (time/minutes offset))]
         (doall
          (for [i (range 0 24)]
            (let [next (time/plus start (time/hours i))
                  hour (time/hour next)]
              ^{:key i} [:div.hour
                         (cond
                           (= hour 0) [:div.new-day
                                       [:div.day (format/unparse
                                                  (format/formatter "dd MMM")
                                                  next)]]
                           :else hour)])))))]))

(defn home-page []
  (let [now (time/now)]
    [:div.hour-rows
     [:div.hour-row [hour-strip now (get (local-date-time-format) "timeZone")]]
     [:div.hour-row [hour-strip now "Europe/Paris"]]
     [:div.hour-row [hour-strip now "Africa/Abidjan"]]
     [:div.hour-row [hour-strip now "Asia/Hong_Kong"]]]))

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

(defn init! []
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (secretary/dispatch! path))
    :path-exists?
    (fn [path]
      (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
