(ns app.ui
  (:require
   [cljs.pprint :refer [pprint]]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.dom :as dom]
   [app.mutations :as api]))

(defsc Person [this {:person/keys [id name age] :as props} {:keys [on-delete]}]
  {:query [:person/id :person/name :person/age]
   :ident (fn [] [:person/id (:person/id props)])
   :initial-state (fn [{:keys [id name age] :as params}]
                    {:person/id id
                     :person/name name
                     :person/age age})}
  (dom/li
   (dom/h5 (str name " (age: " age ")")
           (dom/button {:onClick #(on-delete id)} "X"))))

(def ui-person (comp/factory Person {:keyfn :person/id}))

(defsc PersonList [this {:list/keys [id label people] :as props}]
  {:query [:list/id :list/label {:list/people (comp/get-query Person)}]
   :ident (fn [] [:list/id (:list/id props)])
   :initial-state
   (fn [{:keys [id label]}]
     {:list/id id
      :list/label label
      :list/people (if (= id :friends)
                     [(comp/get-initial-state Person {:id 1 :name "Sally" :age 32})
                      (comp/get-initial-state Person {:id 2 :name "Joe"   :age 22})]
                     [(comp/get-initial-state Person {:id 3 :name "Fred"  :age 11})
                      (comp/get-initial-state Person {:id 4 :name "Baby"  :age 55})])})}
  (letfn [(delete-person [person-id]
            (comp/transact!
             this
             [(api/delete-person {:list/id id
                                  :person/id person-id})]))]
   (dom/div
    (dom/h4 label)
    (dom/ul
     (map #(ui-person (comp/computed % {:on-delete delete-person})) people)))))

(def ui-person-list (comp/factory PersonList))

(defsc Root [this {:keys [friends enemies]}]
  {:query [{:friends (comp/get-query PersonList)}
           {:enemies (comp/get-query PersonList)}]
   :initial-state (fn [params]
                    {:friends (comp/get-initial-state PersonList
                                                      {:id :friends
                                                       :label "Friends"})
                     :enemies (comp/get-initial-state PersonList
                                                      {:id :enemies
                                                       :label "Enemies"})})}
  (dom/div
   {}
   (ui-person-list friends)
   (ui-person-list enemies)))

(comment
  (com.fulcrologic.fulcro.components/get-initial-state app.ui/Root {})
  (pprint (com.fulcrologic.fulcro.application/current-state app.application/app))
  (pprint (fdn/db->tree [{:friends [:list/label]}] (comp/get-initial-state app.ui/Root {}) ))
  (def state (com.fulcrologic.fulcro.application/current-state app.application/app))
  (def query (com.fulcrologic.fulcro.components/get-query app.ui/Root))
  (pprint (com.fulcrologic.fulcro.algorithms.denormalize/db->tree query state state)))
