---
title: Program Overview
summary: This is your hub for understanding how the initiative is unfolding in real time — from idea to execution.
layout: page
last_updated: 2025-03-29
---

## Introduction

_The TMDC Initiative is organized as a **program** composed of multiple time-bound, outcome-oriented projects._
_Each project aligns with a specific implementation phase and contributes toward our collective mission to establish community-owned housing infrastructure._

If the [Initiative Section](/initiative/) explains the theory and phases, this section shows how it's being actively delivered.

### Active & Upcoming Projects

<ul>
{% assign sorted_projects = site.projects | sort: 'quarter' %}
{% for project in sorted_projects %}
  <li>
    <a href="{{ project.url }}">{{ project.title }}</a>
    <small>
      ({{ project.phase }} | {{ project.quarter }} | 
      <strong>{{ project.status | capitalize }}</strong>)
    </small><br />
    <span style="font-size: 0.8em;">{{ project.summary }}</span>
  </li>
{% endfor %}
</ul>

### Program-Wide Success Markers

> We might convert this to a status tracker in the near future. If that sounds cool to you, consider [joining the effort](/contribute/)!

- A minimum of **200 expressions of interest**, with 50% conversion
- **All co-ops legally incorporated** and member-ready
- **Transition of leadership** to elected Development Co-op board
- At least **one multiplex project scoped and build-ready**

<footer>
<div role="group">
  <a class="secondary" role="button" href="/contribute">Learn how you can contribute →</a>
</div>
</footer>
