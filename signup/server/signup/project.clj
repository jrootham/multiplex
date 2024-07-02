(defproject signup "0.1.0-SNAPSHOT"
  :description "Server for multiplex co-ops survey/signup"
  :url "https://jrootham.ca"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies 
  [
  	[org.clojure/clojure "1.11.3"]
		[ring/ring-core "1.12.1"]
		[ring/ring-jetty-adapter "1.12.1"]
		[compojure "1.7.1"]
		[com.draines/postal "2.0.5"]
		[hiccup "2.0.0-RC1"]
		[bananaoomarang/ring-debug-logging "1.1.0"]  	
  ]
  :main ^:skip-aot signup.core
  :target-path "target/%s"
  :profiles 
  {
  	:uberjar 
  	{
			:uberjar-name "signup.jar"
  		:aot :all
  	}
  }
)
