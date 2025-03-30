rm ../site/*
cp ../static/* ../site
lein run ../src ../site site.outline banner $1
