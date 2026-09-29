---
marp: true
theme: default
paginate: true
stream: intro
slot: 10:30-10:40
title: Space primer
---

<!-- _class: lead -->

# A two-minute space primer

### Just enough vocabulary to do the exercises

---

# The columns the exercises use

Every exercise **selects, filters or groups by** these. Six numbers describe an
orbit; four matter here:

| element | means | in our data |
|---|---|---|
| **inclination** | tilt of the orbit: 0° equatorial, 90° polar, >90° retrograde | `inclination` |
| **eccentricity** | 0 = perfect circle, near 1 = very stretched | `eccentricity` |
| **semi-major axis** | the orbit's size — sets the **period** | `semimajor_axis_km` |
| **mean motion** | revolutions per day | `mean_motion` |

The closer the orbit, the faster it goes. **Module 1 asks you to compute the
period** from this: `period = 1440 / mean_motion` minutes.

---

# Two more columns: apogee and perigee

<div class="grid two">
  <div class="tile"><b>perigee</b><span class="d">the lowest point of the orbit &mdash; <code>perigee_km</code></span></div>
  <div class="tile"><b>apogee</b><span class="d">the highest point of the orbit &mdash; <code>apogee_km</code></span></div>
</div>

The exercises plot and sort by **these**, not by `altitude_km` — that one is the
*current* height and is `NaN` for ~1,000 decaying objects. `apogee_km` /
`perigee_km` are always there.

---

# The four classes you group by

`orbit_class` has exactly four values; every group-by in the exercises lands on
them.

<div class="grid two">
  <div class="tile"><b>LEO · Low Earth Orbit</b><span class="d">~160–2,000 km · period 90–130 min · most of the catalog</span></div>
  <div class="tile"><b>MEO · Medium Earth Orbit</b><span class="d">~2,000–35,786 km · period 2–24 h · navigation (GPS, Galileo)</span></div>
  <div class="tile"><b>GEO · Geostationary</b><span class="d">~35,786 km · period ~24 h · fixed over one spot</span></div>
  <div class="tile"><b>HEO · Highly Elliptical</b><span class="d">very stretched · long, slow orbits · science, some comms</span></div>
</div>

**35,786 km** is the geostationary belt — module 3 draws a reference line there.

---

# Payload, debris, rocket body

`satcat_object_type` is one of `PAY` / `DEB` / `R/B`. Module 2 **counts the
debris**, and the bridge exercise **predicts it** from the orbital elements.

Three break-ups dominate:

- **Fengyun-1C** (2007) — an anti-satellite test, ~3,000 fragments
- **Iridium-33 / Cosmos-2251** (2009) — the first accidental satellite collision
- plus every rocket body left behind

They show up as named groups in `norad_groups`
(`fengyun-1c-debris`, `cosmos-2251-debris`, `iridium-33-debris`).

---

# Propagation — for the bonus only

Forget this one unless you do the 3-D bonus notebook. Nothing in modules 1–3
propagates anything.

A TLE is a snapshot at an **epoch**. To know *where* an object is **now**, you
run it forward — that is **orbit propagation**, and **SGP4/SDP4** is the standard
model behind it.

<div class="grid two">
  <div class="tile"><b>elements + time</b><span class="d">→ a position <strong>(x, y, z)</strong> and velocity</span></div>
  <div class="tile"><b>in an inertial frame</b><span class="d">Earth-centred, and <em>not</em> rotating with the ground</span></div>
</div>

---

# The words you will see in the code

| term | one line |
|---|---|
| **TLE / OMM / GP** | ways CelesTrak publishes element sets |
| **epoch** | the instant a TLE is valid for |
| **B\*** (`bstar`) | a drag coefficient — a proxy for "how much air is up there" |
| **RAAN** | where the orbit plane is rotated around the pole |
| **propagation** | advancing an orbit in time (bonus only) |
| **LEO/MEO/GEO/HEO** | the four `orbit_class` values |

That is everything the exercises assume.
