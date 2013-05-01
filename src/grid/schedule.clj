(ns grid.schedule
  (:import [java.util.concurrent TimeUnit Executors TimeoutException]))

(defn- time-unit-lookup [name]
  (case name
    :nanoseconds TimeUnit/NANOSECONDS
    :microseconds TimeUnit/MICROSECONDS
    :milliseconds TimeUnit/MILLISECONDS
    :seconds TimeUnit/SECONDS
    :minutes TimeUnit/MINUTES
    :hours TimeUnit/HOURS))

(defn- new-pool
  "Initializes a scheduled thread pool. This pool holds up to 32 threads when idle, to speed up task startup."
  []
  (Executors/newScheduledThreadPool 32))

(def schedule-pool (new-pool))

(defn schedule
  "Schedules a task. args is a list of keyword arguments:
  - :delay specifies the amount of time units before this task runs. By default, this is 0.
  - :repeat, when set, specifies the amount of time units between repeated executions of this task (which can overlap). When not set, the task will run only once. By default this is not set.
  - :time-unit specifies the kind of time unit used when specifying delay or repeat. Possible values are (:nanoseconds :microseconds :milliseconds :seconds :minutes :hours). By default this is :milliseconds.

  The object returned by this function can be used in a subsequent unschedule call to cancel the scheduling."
  [task & {:as args}]
  (let [{:keys [delay repeat time-unit]
         :or {delay 0
              time-unit :milliseconds}} args]
    (if repeat
      (.scheduleAtFixedRate schedule-pool
                            task
                            delay
                            repeat
                            (time-unit-lookup time-unit))

      (.schedule schedule-pool
                 task
                 delay
                 (time-unit-lookup time-unit)))))

(defn unschedule
  "unschedules a task that was previously returned by schedule. The optional force argument is a boolean specifying whether or not to halt the task if it was already running. The default is false."
  [task &[force]]
  (.cancel task (boolean force))
  nil)

(defn reset-scheduler!
  "Resets the scheduler, unscheduling all future tasks but allowing running tasks to finish. Optionally takes a timeout value (defaulting to 5000) and a time unit (defaulting to :milliseconds), specifying how long to wait for tasks to finish. The time units are the same as the ones for schedule. If the operation times out, a TimeoutException will be thrown."
  [& [timeout time-unit]]
  (.shutdown schedule-pool)
  (when (not (.awaitTermination schedule-pool
                                (or timeout 5000)
                                (if time-unit
                                  (time-unit-lookup time-unit)
                                  (time-unit-lookup :milliseconds))))
    (throw (TimeoutException. "Thread pool shutdown timed out.")))
  (def schedule-pool (new-pool))
  nil)

(defn emergency-reset!
  "Resets the thread pool, unscheduling all future tasks and halting all current tasks."
  []
  (.shutdownNow schedule-pool)
  (def schedule-pool (new-pool))
  nil)

