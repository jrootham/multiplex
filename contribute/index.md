---
title: Get Involved
summary: How to contribute to the effort
description: There are many ways to contribute to the Toronto Multiplex Development Co-operative (TMDC) — as a future resident, a volunteer, a technical partner, an investor, or an ally. This page outlines the paths to participation and how you can plug in.
layout: page
---

## ✨ Together, We Can Reimagine Housing

This initiative isn’t just about buildings — it’s about creating a new system. A better future takes many hands.

Join us in building it.

## How to Contribute

Whether you’re a prospective resident, a technical expert, a builder, a neighbor, or a values-aligned investor — **you have a role to play** in shaping the future of housing in Toronto.

### Households: Join the Movement

If you want to live in a co-op multiplex — or even just support the idea — **preregister to join our future housing co-ops**. This helps us:

- Measure demand
- Demonstrate traction
- Reach our 200+ household milestone for launch

[Register Your Interest](/register)

### Volunteers: Join the Build Crew

We are a community-powered initiative. Volunteers contribute to everything from communications to architecture research.

Check out our current needs:

<ul>
    {% for project in site.projects %}
      {% assign open_roles = project.team | where: "status", "Needed" %}
      {% if open_roles.size > 0 %}
        <li>Project: <a href="{{ project.url }}">{{ project.title }}</a>
          <ul>
            {% for role in open_roles %}
              <li>{{ role.role }}</li>
            {% endfor %}
          </ul>
        </li>
      {% endif %}
    {% endfor %}
  </ul>

When you’re ready:

- Join our contributor Slack (link coming soon)
- Connect with our volunteer coordinator
- Start with a project or working group that fits your skills

### Prospective Partners

Are you an architect, builder, community land trust, legal expert, or policy advisor?

We are actively developing strategic relationships with:

- Designers & developers
- Community land trusts
- Legal & governance advisors
- Technical support cooperatives

Get in touch with our partnership lead: [email@address.xyz](mailto:email@address.xyz)

### Community Bond Investors

We’ll soon launch a community bond campaign to fund the first housing co-op. You’ll be able to:

- Support ethical, community-owned development
- Receive a fair return on investment

Sign up for our mailing list to stay informed:
Link coming soon.

### Press & Media Inquiries

Interested in covering TMDC or exploring the story behind the initiative?

Reach us at [email@address.xyz](mailto:email@address.xyz)
