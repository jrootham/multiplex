(defproject makesite "0.1.0-SNAPSHOT"
  :description "Create the website from markdown files"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies 
  [
  	[org.clojure/clojure "1.11.3"]
  	[hiccup "2.0.0-RC1"]
  	[commonmark-hiccup "0.3.0"]
  	[com.atlassian.commonmark/commonmark-ext-gfm-tables "0.17.0"]
  ]
  :main ^:skip-aot makesite.core
  :target-path "target/%s"
  :profiles 
  {
  	:uberjar 
  	{
  		:uberjar-name "makesite.jar"
  		:aot :all
  	}
  }
)
