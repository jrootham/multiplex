## 📁 Project Structure

This Jekyll-based site includes the following key folders:

- `_data/` – Data files to be used across the site
- `_includes/` – Reusable HTML partials (like headers, footers, etc.)
- `_layouts/` – Page templates used by your Markdown or HTML files
- `_posts/` – Blog-style or news posts, organized by date (more of these _Collections_ can be configured as needed)
- `assets/` – Static files like images, stylesheets, or JavaScript

And key root-level files:

- `_config.yml` – Main configuration for Jekyll
- `.ruby-version` – Specifies the Ruby version (`3.2.2`)
- `*.html` and `*.md` – Instantiate actual site content pages
- `Gemfile` and `Gemfile.lock` – Ruby gem dependencies

---

## 🚀 Getting Started

To run the site locally, follow these setup steps:

### 1. Install Ruby (with a version manager)

It's best to manage Ruby versions with a version manager so your system can easily switch between different versions for different projects.
The example is with [**rbenv**](https://github.com/rbenv/rbenv), alternatives: [**rvm**](https://rvm.io/), [**asdf**](https://asdf-vm.com/).

To install Ruby 3.2.2 using **rbenv**:

```sh
rbenv install 3.2.2
rbenv local 3.2.2
```

> This creates a `.ruby-version` file in the project root, telling the system to use Ruby 3.2.2 when inside this folder.

### 2. Install project dependencies

**Bundler** is a Ruby dependency manager use to install the libraries like Jekyll. Bundler is typically included with a standard Ruby installation.

Install Project dependencies:

```sh
bundle install
```

> This command reads the `Gemfile`, installs the required gems, and locks them into `Gemfile.lock` for consistent builds.

### 3. Run the local development server

To preview the site locally, run:

```sh
bundle exec jekyll serve
```

> This starts a local web server using Jekyll. The `bundle exec` part ensures the correct versions of the gems (like Jekyll) from the `Gemfile.lock` are used.

Once the server starts, visit: [http://localhost:4000](http://localhost:4000)

You should see the site running locally. Changes to content or layouts will automatically reload in your browser.

---

## 📌 Notes

- This is a **work in progress**.
- Content and layout will evolve rapidly.
