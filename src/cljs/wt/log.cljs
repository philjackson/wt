(ns wt.log)

(defn log [message]
  (.log js/console (with-out-str
                     (cljs.pprint/pprint message)))
  message)
