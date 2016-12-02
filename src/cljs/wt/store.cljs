(ns wt.store)

(defn from-ls [key]
  (some->> (.getItem js/localStorage key)
           (cljs.reader/read-string)))

(defn to-ls [key value]
  (.setItem js/localStorage key (str value)))
