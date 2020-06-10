(ns frodo.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [frodo.core-test]))

(doo-tests 'frodo.core-test)

