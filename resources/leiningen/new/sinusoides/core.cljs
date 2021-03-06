;; Copyright (c) 2016 FIXME
;;
;; This file is part of {{name}}.
;;
;; {{name}} is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as
;; published by the Free Software Foundation, either version 3 of the
;; License, or (at your option) any later version.
;;
;; {{name}} is distributed in the hope that it will be useful, but
;; WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
;; Affero General Public License for more details.
;;
;; You should have received a copy of the GNU Affero General Public
;; License along with Mittagessen.  If not, see
;; <http://www.gnu.org/licenses/>.

(ns {{name}}.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! timeout]]
            [cljs.core.match :refer-macros [match]]
            [cljs.core.match]
            [clojure.string :as str]
            [secretary.core :as secretary :refer-macros [defroute]]
            [pushy.core :as pushy])
  (:import goog.History))


;;
;;  Application state
;;  =================
;;

(defonce app-state
  (r/atom {:data nil
           :view [:init]}))

;;
;; Views
;; =====
;;

(defn main-view [state]
  (r/with-let
    [_ (go (let [data (:body (<! (http/get "data/data.json")))]
             (swap! state assoc-in [:data] data)))]
    [:div#main
     [:h1 "welcome to {{name}}"]
     (str (:data @state))]))

(defn not-found-view []
  [:div#not-found
   [:h1 "Page not found!"]])

(defn root-view [state]
  (match [(:view @state)]
         [[:init]]  [:div]
         [[:main]]  [main-view state]
         :else      [not-found-view]))

(defn init-components! [app-state]
  (r/render-component
   [root-view app-state]
   (.getElementById js/document "components")))


;;
;; Routing
;; =======
;;

(defonce history (atom))

(defn nav! [token]
  (pushy/set-token! @history token))

(defn make-routes! [app]
  (defroute route-main "/" []
    (swap! app assoc-in [:view] [:main]))
  (defroute route-not-found "/not-found" []
    (swap! app assoc-in [:view] [:not-found])))

(defn- no-prefix [uri]
  (let [prefix (secretary/get-config :prefix)]
    (str/replace uri (re-pattern (str "^" prefix)) "")))

(defn init-history! []
  (when (aget js/window "{{uppercased}}_DEBUG_MODE")
    (print "{{uppercased}}_DEBUG_MODE enabled")
    (secretary/set-config! :prefix "/debug"))
  (letfn [(dispatch [uri] (secretary/dispatch! uri))
          (match-uri [uri]
            (if (secretary/locate-route (no-prefix uri))
              uri
              (route-not-found)))]
        (reset! history (pushy/pushy dispatch match-uri))
        (pushy/start! @history)))

(defn init-router! [app-state]
  (make-routes! app-state)
  (init-history!))


;;
;; Application
;; ===========
;;

(defn init-app! []
  (enable-console-print!)
  (prn "{{name}} app started!")
  (init-router! app-state)
  (init-components! app-state))

(defn on-figwheel-reload! []
  (prn "Figwheel reloaded...")
  (swap! app-state update-in [:__figwheel_counter] inc))

(init-app!)
