(ns helix.core
  (:refer-clojure :exclude [type])
  (:require [goog.object :as gobj]
            [helix.impl.props :as impl.props]
            [helix.impl.classes]
            [cljs-bean.core :as bean]
            ["react" :as react])
  (:require-macros [helix.core]))


(when (exists? js/Symbol)
  (extend-protocol IPrintWithWriter
    js/Symbol
    (-pr-writer [sym writer _]
      (-write writer (str "\"" (.toString sym) "\"")))))


(def Fragment react/Fragment)


(def Suspense react/Suspense)


(def create-element react/createElement)


(def create-context react/createContext)


;; this is to enable calling `(.createElement (get-react))` without doing
;; a dynamic arity dispatch. See https://github.com/Lokeh/helix/issues/20
(defn get-react [] react)


(defn $
  "Create a new React element from a valid React type.

  Example:
  ```
  ($ MyComponent
   \"child1\"
   ($ \"span\"
     {:style {:color \"green\"}}
     \"child2\" ))
  ```"
  [type & args]
  (let [?p (first args)
        ?c (rest args)
        native? (or (keyword? type)
                    (string? type)
                    (:native (meta type)))
        type' (if (keyword? type)
                (name type)
                type)]
    (if (map? ?p)
      (apply create-element
             type'
             (if native?
               (impl.props/-native-props ?p)
               (impl.props/-props ?p))
             ?c)
      (apply create-element
             type'
             nil
             args))))


(def ^:deprecated $$
  "Dynamically create a new React element from a valid React type.

  `$` can typically be faster, because it will statically process the arguments
  at macro-time if possible.

  Example:
  ```
  ($$ MyComponent
   \"child1\"
   ($$ \"span\"
     {:style {:color \"green\"}}
     \"child2\" ))
  ```"
 $)


(defprotocol IExtractType
  (-type [factory] "Extracts the underlying type from the factory function."))


(defn type
  [f]
  (-type f))


(defn factory
  "Creates a factory function for a React component"
  [type]
  (-> (fn factory [& args]
        (apply $ type args))
      (specify! IExtractType
        (-type [_] type))))


(defn- cljs-factory
  [type]
  (-> (fn factory [& args]
        ;; put props detection here so it's easier to detect
        ;; slow paths in render
        (if (map? (first args))
          (apply react/createElement
                 type
                 #js {"helix/props" (first args)}
                 (rest args))
          (apply react/createElement
                 type
                 #js {}
                 args)))))


(defn- extract-cljs-props
  [o]
  (when (and ^boolean goog/DEBUG (map? o))
    (throw (ex-info "Props received were a map. This probably means you're calling your component as a function." {:props o})))
  (if-let [props (gobj/get o "helix/props")]
    (assoc props :children (gobj/get o "children"))
    (bean/bean o)))



;;
;; -- class components
;;



(defn create-component [spec statics]
  (let [render (.-render ^js spec)
        render' (fn [this]
                  (render
                   this
                   (extract-cljs-props (.-props ^js this))
                   (.-state ^js this)))]
    (gobj/set spec "render" render')
    (helix.impl.classes/createComponent react/Component spec statics)))

(comment
  (def MyComponent
    (create-component #js {:displayName "Foo"
                           :constructor
                           (fn [this]
                             (set! (.-state this) #js {:count 3}))
                           :render
                           (fn [this props state]
                             (prn props state)
                             ($$ "div" (.-count (.-state this))))}
                      nil))

  (js/console.log MyComponent)

  (rds/renderToString ($$ MyComponent {:foo "baz"})))


;;
;; -- React Fast Refresh
;;


(defn register!
  "Registers a component with the React Fresh runtime.
  `type` is the component function, and `id` is the unique ID assigned to it
  (e.g. component name) for cache invalidation."
  [type id]
  (when (exists? (.-$$Register$$ js/window))
    (.$$Register$$ js/window type id)))


(defn signature! []
  ;; grrr `maybe` bug strikes again
  (and (exists? (.-$$Signature$$ js/window))
       (.$$Signature$$ js/window)))
