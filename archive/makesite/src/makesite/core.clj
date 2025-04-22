(ns makesite.core
  (:gen-class)
  (:require [clojure.string :as string])
  (:require [clojure.java.io :as io])
	(:require [hiccup2.core :as hiccup])
	(:require [hiccup.page :as page])
	(:require [hiccup.element :as element])
	(:require [commonmark-hiccup.core :as markdown])
)

(defn debug [value]
	(println value)
	value
)

(defn debug1 [name value]
	(println name value)
	value
)

(def markdown-config
	(-> markdown/default-config
		(update-in 
			[:parser :extensions] 
			conj
			(org.commonmark.ext.gfm.tables.TablesExtension/create)
		)
		(update-in [:renderer :nodes] merge
			{
				org.commonmark.ext.gfm.tables.TableBlock [:table :content]
				org.commonmark.ext.gfm.tables.TableHead  [:thead :content]
				org.commonmark.ext.gfm.tables.TableBody  [:tbody :content]
				org.commonmark.ext.gfm.tables.TableRow   [:tr :content]
				org.commonmark.ext.gfm.tables.TableCell  [:td :content]
			}
		)
	)
)

(defn pitch [text]
	(if (string/starts-with? text "# Pitch")
		(println text)
	)
)

(defn markdown-to-hiccup [text version]
	(let [translated (string/replace text "{{version}}" version)]
		(markdown/markdown->hiccup markdown-config translated)
	)
)

(def head
	[:head
		[:meta {:charset "utf-8"}]
		[:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
		(page/include-css "common.css")
		(page/include-css "site.css")
		[:title "Toronto Multiplex Co-ops"]
	]
)

(defn make-top-link [link]
	(let 
		[
			label (first link)
			href (str (first (rest link)) ".html")
		]
		(list 
			[:div {:class "top-space"}]
			[:div {:class "top-link"} (element/link-to href label)]
			[:div {:class "top-space"}]
		)
	)
)

(defn make-top-html [banner top]
	[:div {:class "top"} [:div {:class "banner"} banner] [:div {:class "top-nav"} (map make-top-link top)]]
)

(defn compute-indent [row]
	(count (filter (fn [element] (= "" element)) row))
)

(defn has-name? [side page indent]
	(if (= (last (first side)) page)
		true
		(if (> (compute-indent (first (rest side))) indent)
			(has-name? (rest side) page indent)
			false
		)
	)
)

(defn is-display-branch? [side page indent]
	(let [more (rest side)]
		(if (empty? more)
			false
			(if (> (compute-indent (first more)) indent)
				(has-name? side page indent)
				false
			)
		)
	)
)

(defn side-block [contents]
	[:div {:class "indent"} contents]
)

(defn make-side-link [row]
	(let 
		[
			stripped (filter (fn [element] (not= "" element)) row)
			label (first stripped)
			name (last stripped)
		]
		[:div {:class "link"} (element/link-to (str name ".html") label)]
	)
)

(defn next-link [side indent]
	(if (empty? side)
		side
		(let [candidate (rest side)]
			(if (empty? candidate)
				candidate
				(if (<= (compute-indent (first candidate)) indent)
					candidate
					(next-link candidate indent)
				)
			)
		)
	)
)

(declare side-vector)

(defn branch [side page indent]
	(let [tree (side-block (side-vector side page (+ 1 indent)))]
		(cons tree (side-vector (next-link side indent) page indent))
	)
)

(defn tail [side page indent]
	(if (is-display-branch? side page indent)
		(branch (rest side) page indent)
		(side-vector (next-link side indent) page indent)
	)
)

(defn side-vector [side page indent]
	(if (empty? side) 
		nil
		(let 
			[
				current (first side)
				link (make-side-link current)
				current-indent (compute-indent current)
			]
			(if (> indent current-indent)
				nil
				(cons link (tail side page indent))
			)
		)
	)
)

(defn make-side-html [side page]
	[:div 
		{:class "side-nav"} 
		[:div 
			{:class "side-header"} 
			[:h3 "Navigation"]
		]
		(side-block (side-vector side page 0))
	]
)

(defn make-body [banner-input input page top side version]
	(let
		[
			banner-hiccup (markdown-to-hiccup (slurp banner-input) version)
			top-html (make-top-html banner-hiccup top)
			side-html (make-side-html side page)
			contents-html [:div {:class "contents" :id page} (markdown-to-hiccup (slurp input) version)]
		]
		[:body [:div {:class "outer"} top-html [:div {:class "columns"} side-html contents-html]]]
	)
)

(defn write-a-page [source destination banner page top side version]
	(let 
		[
			name (str page ".md")
			banner-name (str banner ".md")
			input (io/file source name)
			banner-input (io/file source banner-name)
			output (io/file destination (str page ".html"))
		]
		(if (.exists banner-input)
			(if (.exists input)
				(spit output (page/html5 head (make-body banner-input input page top side version)))
				(println name " does not exist")
			)
			(println banner-name " does not exist")
		)
	)
)

(defn write-site [source destination banner top side contents version]
	(let 
		[
			write-page (fn [page] (write-a-page source destination banner page top side version))
		]
		(doall (map write-page contents))
	)
)

(defn split-line [line]
	(string/split line #"\t")
)

(defn remove-comments [line]
	(not (string/starts-with? (string/trim line) "#"))
)

(defn remove-blanks [line]
	(not (string/blank? line))
)

(defn tokenize [source outline]
	(with-open [reader (io/reader (io/file source outline))]
		(map split-line (filter remove-comments (filter remove-blanks (doall (line-seq reader)))))
	)
)

(defn side? [row]
	(< 2 (count row))
)

(defn top? [row]
	(= 2 (count row))
)

(defn md-filter [name]
	(false? (string/starts-with? name "/"))
)

(defn name-map [row]
	(last row)
)

(defn makesite [src outline dest banner version]
	(let 
		[
			source (io/file src)
			destination (io/file dest)
		]
		(if (.isDirectory source)
			(if (.isDirectory destination)
				(let [tokens (tokenize source outline)]
					(let
						[
							top (filter top? tokens)
							side (map rest (filter side? tokens))
							contents (filter md-filter (map name-map tokens))
						]
						(write-site source destination banner top side contents version)
					)
				)
				(println dest " is not a directory")
			)
			(println src " is not a directory")
		)
	)
)

(defn -main
  "Make a static site from a site description file and a set of .md files"
  [& args]
  (if (= 5 (count args))
  	(let
  		[
  			src (nth args 0)
  			dest (nth args 1)
  			outline (nth args 2)
  			banner (nth args 3)
  			version (nth args 4)
  		]
  		(makesite src outline dest banner version)
  	)
  	(do
	  	(println "java -jar makesite.jar src dest site.outline banner version")
	  	(println (count args))
	  	(println args)
  	)
	)
)
