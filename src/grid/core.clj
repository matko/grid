(ns grid.core
  (:use quil.core grid.schedule)
  (:import [java.util.concurrent TimeUnit Executors])
  (:gen-class))

(defn setup []
  (smooth)
  (frame-rate 60))

(defn grid [height width]
  (into {}
   (for [x (range height)
         y (range width)]
     [[x y] (atom {:color [255 0 0]})])))
    
(defn setup []
  (def moo (grid 10 10)))

(def cell-size 25)

(defn draw []
  (frame-rate 60)
  (background 200)
  (stroke 0 0 0)
  (stroke-weight 2)
  (doseq [[[x y] cell] moo]
    (apply fill (:color @cell))
    (rect (* cell-size x)
          (* cell-size y)
          cell-size cell-size)))

(def colors {:red [255 0 0]
             :green [0 255 0]
             :blue [0 0 255]
             :white [255 255 255]
             :black [0 0 0]})

(defn random-pos [& _]
  (vector (int (rand 10))
                     (int (rand 10))))

(defn randcolor-fn [color]
  (let [pos (atom (random-pos))]
    (fn []
      (swap! (moo @pos)
             #(assoc %
                :color (:red colors)))
      (swap! pos random-pos)
      (swap! (moo @pos)
             #(assoc %
                :color color)))))

(defn random-color []
  (vec (map (comp int rand)
            (repeat 3 256))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (defsketch animation
    :title "Grids are great fun"
    :setup setup
    :draw #(draw)
    :key-typed #(System/exit 0)
    :size [250 250])
  (dotimes [i 1000]
    (schedule (randcolor-fn (random-color))
              :repeat 100
              :time-unit :milliseconds)))
