---
marp: true
theme: default
paginate: true
title: Space primer
---

<!-- _class: lead -->

# A two-minute space primer

### Just enough vocabulary to read the plots

---

# What is in the catalog

<div class="grid three">
  <div class="tile"><b>payload</b><span class="d">a working satellite (PAY)</span></div>
  <div class="tile"><b>debris</b><span class="d">a fragment, often from a collision or break-up (DEB)</span></div>
  <div class="tile"><b>rocket body</b><span class="d">the spent upper stage that delivered something (R/B)</span></div>
</div>

Every object gets a **NORAD catalog number** — a permanent id. That is the
`norad_cat_id` column, and the key everything joins on.

A **TLE** (Two-Line Element set) is the compact, fixed-width text encoding of an
object's orbit. `tle_line1` and `tle_line2` in our file *are* those two lines.

---

# An orbit is an ellipse

Six numbers describe it. Four matter for this lab:

| element | means | in our data |
|---|---|---|
| **inclination** | tilt of the orbit: 0° equatorial, 90° polar, >90° retrograde | `inclination` |
| **eccentricity** | 0 = perfect circle, near 1 = very stretched | `eccentricity` |
| **semi-major axis** | the orbit's size — sets the **period** | `semimajor_axis_km` |
| **mean motion** | revolutions per day | `mean_motion` |

The closer the orbit, the faster it goes. That is why **mean motion alone tells
you the period**: `period = 1440 / mean_motion` minutes.

---

# Near the Earth: altitude

<div class="grid two">
  <div class="tile"><b>perigee</b><span class="d">the lowest point of the orbit — <code>perigee_km</code></span></div>
  <div class="tile"><b>apogee</b><span class="d">the highest point of the orbit — <code>apogee_km</code></span></div>
</div>

In this file `altitude_km` is the *current* height from propagation, and it is
`NaN` for ~1,000 decaying objects. `apogee_km` / `perigee_km` are always there.

---

# The four families

<div class="grid two">
  <div class="tile"><b>LEO · Low Earth Orbit</b><span class="d">~160–2,000 km · period 90–130 min · most of the catalog</span></div>
  <div class="tile"><b>MEO · Medium Earth Orbit</b><span class="d">~2,000–35,786 km · period 2–24 h · navigation (GPS, Galileo)</span></div>
  <div class="tile"><b>GEO · Geostationary</b><span class="d">~35,786 km · period ~24 h · fixed over one spot</span></div>
  <div class="tile"><b>HEO · Highly Elliptical</b><span class="d">very stretched · long, slow orbits · science, some comms</span></div>
</div>

**35,786 km** is the geostationary belt: at that height the period is exactly one
day, so a satellite there hovers over a fixed point. One horizontal line on the
plot and you can *see* the band.

---

# Why debris dominates

Three collisions define the debris population in our data:

- **Fengyun-1C** (2007) — an anti-satellite test, ~3,000 fragments
- **Iridium-33 / Cosmos-2251** (2009) — the first accidental satellite collision
- plus every rocket body left behind

You can see them as named groups in `norad_groups`
(`fengyun-1c-debris`, `cosmos-2251-debris`, `iridium-33-debris`).

---

# Propagation: from elements to a position

A TLE is a snapshot at an **epoch**. To know *where* an object is **now**, you
run it forward — that is **orbit propagation**, and **SGP4/SDP4** is the standard
model behind it.

<div class="grid two">
  <div class="tile"><b>elements + time</b><span class="d">→ a position <strong>(x, y, z)</strong> and velocity</span></div>
  <div class="tile"><b>in an inertial frame</b><span class="d">Earth-centred, and <em>not</em> rotating with the ground</span></div>
</div>

That is the whole trick in the bonus notebook: propagate all 21,207 objects to
the current instant with **Orekit**, then plot the resulting point cloud in 3D.

---

# Vocabulary you can now use

| term | one line |
|---|---|
| **TLE / OMM / GP** | ways CelesTrak publishes element sets |
| **epoch** | the instant a TLE is valid for |
| **B\*** (`bstar`) | a drag coefficient — a proxy for "how much air is up there" |
| **RAAN** | where the orbit plane is rotated around the pole |
| **propagation** | advancing an orbit in time |
| **inertial frame** | coordinate axes fixed relative to the stars, not the ground |
| **LEO/MEO/GEO/HEO** | the four altitude families |

That is everything the notebooks assume.
