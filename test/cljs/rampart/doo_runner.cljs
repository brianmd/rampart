(ns rampart.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [rampart.core-test]))

(doo-tests 'rampart.core-test)

