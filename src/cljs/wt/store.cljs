(ns wt.store)

(defn from-ls [key]
  (some->> (.getItem (.-localStorage js/window) key)
           (cljs.reader/read-string)))

(defn to-ls [key value]
  (.setItem (.-localStorage js/window) key (str value)))
