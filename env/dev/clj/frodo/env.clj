(ns frodo.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [frodo.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[frodo started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[frodo has shut down successfully]=-"))
   :middleware wrap-dev})
