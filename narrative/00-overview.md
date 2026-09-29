---
marp: true
theme: default
paginate: true
stream: intro
slot: 10:30-10:40
title: Overview
---

<!-- _class: lead -->

# Java is for Data Science, Too

### Building an end-to-end ML pipeline without leaving the JVM

**Devoxx Belgium 2026 · hands-on lab**
Sven Reimers · Zoran Sevarac

---

# The big agenda

<div class="agenda">
  <div class="slot"><span class="t">10:30</span><span class="m">intro &amp; the data</span></div>
  <div class="slot"><span class="t">10:40</span><span class="m">M1 · JTaccuino</span></div>
  <div class="slot"><span class="t">10:55</span><span class="m">M2 · Hardwood + dflib</span></div>
  <div class="slot"><span class="t">11:10</span><span class="m">M3 · gog4j</span></div>
  <div class="slot z"><span class="t">11:25</span><span class="m">ML pipeline · Zoran</span></div>
  <div class="slot"><span class="t">12:15</span><span class="m">wrap-up &amp; questions</span></div>
</div>

<div class="sep">Part 1 &middot; tooling &mdash; Sven &middot; 10:30&ndash;11:25</div>

<div class="grid two">
  <div class="tool">
    <span class="ico"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="3" width="16" height="18" rx="2"/><path d="M8 3v18"/><path d="M12 8h5M12 12h5"/></svg></span>
    <span class="txt"><b>JTaccuino</b><span>a Java REPL, in cells</span></span>
  </div>
  <div class="tool">
    <span class="ico"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><ellipse cx="12" cy="5.5" rx="7" ry="3"/><path d="M5 5.5v13c0 1.7 3.1 3 7 3s7-1.3 7-3v-13"/><path d="M5 12c0 1.7 3.1 3 7 3s7-1.3 7-3"/></svg></span>
    <span class="txt"><b>Hardwood</b><span>read the Parquet</span></span>
  </div>
  <div class="tool">
    <span class="ico"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 9h18M9 9v11M15 9v11"/></svg></span>
    <span class="txt"><b>dflib</b><span>shape the data</span></span>
  </div>
  <div class="tool">
    <span class="ico"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4v16h16"/><path d="M8 16v-4M12 16V8M16 16v-6"/></svg></span>
    <span class="txt"><b>gog4j</b><span>draw it</span></span>
  </div>
</div>

<div class="sep ml">Part 2 &middot; ML pipeline &mdash; Zoran &middot; 11:25&ndash;12:15</div>

<div class="alt">
  <div class="tool">
    <span class="ico"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="6" cy="7" r="2"/><circle cx="6" cy="17" r="2"/><circle cx="18" cy="12" r="2.2"/><path d="M8 7.8l7.8 3.1M8 16.2l7.8-3.1"/></svg></span>
    <span class="txt"><b>DeepNetts</b><span>a deep-learning library for the JVM &mdash; a stream of its own</span></span>
    <span class="badge">Zoran's stream</span>
  </div>
</div>

<p class="note">Everything after this slide is <strong>the tooling stream</strong>; the ML pipeline is Zoran's, on his own terms.</p>

---

# Heads-up: the CelesTrak data

Every current element set from <a href="https://celestrak.org">celestrak.org</a>, merged to one row per object and enriched with SATCAT metadata.

<div class="grid three">
  <div class="card"><span class="n">21,207</span><span class="l">objects in orbit</span></div>
  <div class="card"><span class="n">52</span><span class="l">columns</span></div>
  <div class="card"><span class="n">19.2 &rarr; 5.8</span><span class="l">MB &middot; CSV &rarr; Parquet</span></div>
</div>

<div class="grid two">
  <div class="tile">
    <b>By orbit class</b>
    <div class="bars">
      <div class="bar"><span class="lab">LEO</span><span class="track"><span class="fill" style="width:100%"></span></span><span class="val">19,260</span></div>
      <div class="bar"><span class="lab">GEO</span><span class="track"><span class="fill" style="width:6.1%"></span></span><span class="val">1,183</span></div>
      <div class="bar"><span class="lab">HEO</span><span class="track"><span class="fill" style="width:2.9%"></span></span><span class="val">558</span></div>
      <div class="bar"><span class="lab">MEO</span><span class="track"><span class="fill" style="width:1.1%"></span></span><span class="val">206</span></div>
    </div>
  </div>
  <div class="tile">
    <b>By kind</b>
    <div class="bars">
      <div class="bar"><span class="lab">payload</span><span class="track"><span class="fill warm" style="width:100%"></span></span><span class="val">17,110</span></div>
      <div class="bar"><span class="lab">debris</span><span class="track"><span class="fill warm" style="width:17.3%"></span></span><span class="val">2,957</span></div>
      <div class="bar"><span class="lab">rocket b.</span><span class="track"><span class="fill warm" style="width:3.2%"></span></span><span class="val">544</span></div>
    </div>
  </div>
</div>

<p class="note">Every object has a <strong>NORAD catalog number</strong> and a <strong>TLE</strong> (<code>tle_line1</code>/<code>tle_line2</code>) describing its orbit &mdash; a <strong>payload</strong> is a working satellite (PAY), <strong>debris</strong> a fragment (DEB), a <strong>rocket body</strong> a spent stage (R/B). Source: <a href="https://celestrak.org">CelesTrak</a>.</p>
