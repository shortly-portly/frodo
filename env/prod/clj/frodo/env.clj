(ns frodo.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[frodo started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[frodo has shut down successfully]=-"))
   :middleware identity})
