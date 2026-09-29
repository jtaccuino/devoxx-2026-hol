---
marp: true
theme: default
paginate: true
title: Overview
---

<!-- _class: lead -->

# Java is for Data Science, Too

### Building an end-to-end ML pipeline without leaving the JVM

**Devoxx Belgium 2026 · hands-on lab**
Sven Reimers · Zoran Sevarac

---

# The big agenda

<div class="cards">
  <div class="card"><span class="n">10:30</span><span class="l">intro &amp; the data</span><span class="w">Sven</span></div>
  <div class="card"><span class="n">10:40</span><span class="l">module 1 · JTaccuino</span><span class="w">Sven</span></div>
  <div class="card"><span class="n">10:55</span><span class="l">module 2 · Hardwood + dflib</span><span class="w">Sven</span></div>
</div>

<div class="cards">
  <div class="card"><span class="n">11:10</span><span class="l">module 3 · gog4j</span><span class="w">Sven</span></div>
  <div class="card"><span class="n">11:25</span><span class="l">the ML pipeline</span><span class="w">Zoran</span></div>
  <div class="card"><span class="n">12:15</span><span class="l">wrap-up &amp; questions</span><span class="w">both</span></div>
</div>

<p class="note">Every module: a short talk, then <strong>you type</strong>. Solutions are provided.</p>

---

# Heads-up: the data

<div class="cards">
  <div class="card"><span class="n">21,207</span><span class="l">objects in orbit</span></div>
  <div class="card"><span class="n">52</span><span class="l">columns</span></div>
  <div class="card"><span class="n">5.4 MB</span><span class="l">one Parquet file</span></div>
</div>

<div class="cols">
  <div>
    <h3>By orbit class</h3>
    <div class="bars">
      <div class="bar"><span class="lab">LEO</span><span class="track"><span class="fill" style="width:100%"></span></span><span class="val">19,260</span></div>
      <div class="bar"><span class="lab">GEO</span><span class="track"><span class="fill" style="width:6.1%"></span></span><span class="val">1,183</span></div>
      <div class="bar"><span class="lab">HEO</span><span class="track"><span class="fill" style="width:2.9%"></span></span><span class="val">558</span></div>
      <div class="bar"><span class="lab">MEO</span><span class="track"><span class="fill" style="width:1.1%"></span></span><span class="val">206</span></div>
    </div>
  </div>
  <div>
    <h3>By kind</h3>
    <div class="bars">
      <div class="bar"><span class="lab">payload</span><span class="track"><span class="fill warm" style="width:100%"></span></span><span class="val">17,110</span></div>
      <div class="bar"><span class="lab">debris</span><span class="track"><span class="fill warm" style="width:17.3%"></span></span><span class="val">2,957</span></div>
      <div class="bar"><span class="lab">rocket b.</span><span class="track"><span class="fill warm" style="width:3.2%"></span></span><span class="val">544</span></div>
    </div>
  </div>
</div>

<p class="note">Every current CelesTrak element set, merged to one row per object and enriched with SATCAT metadata (owner, object type, launch date).</p>
