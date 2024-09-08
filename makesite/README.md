## makesite

### Usage

    $ java -jar makesite.jar source destination site.outline banner

|Argument|Meaning|
|----|----|
|source|Source directory (.md files)|
|destination|Destination directory|
|site.outline|Site description file (in source)|
|banner|Name of banner .md file (without extension)|

### Input (.md) file spec

Markdown [translator](https://github.com/yogthos/markdown-clj) plus tables.

### Input (site.outline) file spec

Tab separated file.

Last string in each line is the name of a .md file (without extension).

Each file gets translated to HTML and embedded in a div with class "contents" and id "*name*".

A line that just has the name creates an unlinked HTML file.

A line with just a label and a name is linked to the top navigation bar.

A line with a leading tab is linked to the side navigation bar.

Each additional leading tab nests the link one more level.

### Output (HTML) file 

There is a constant header.

A banner which is the translated banner.md file.

The top navigation div.

The side navigation div is next to the contents div.




