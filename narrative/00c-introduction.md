---
marp: true
theme: default
paginate: true
title: Introduction
---

<!-- _class: lead -->

# What we will be doing

### One small Java program, end to end

---

# What you will build

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

<p class="note">One pipeline over the CelesTrak catalog &mdash; <strong>no Python, no context switch, just the JVM.</strong></p>

<div class="sep">and a separate stream</div>

<div class="alt">
  <div class="tool">
    <span class="ico"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="6" cy="7" r="2"/><circle cx="6" cy="17" r="2"/><circle cx="18" cy="12" r="2.2"/><path d="M8 7.8l7.8 3.1M8 16.2l7.8-3.1"/></svg></span>
    <span class="txt"><b>DeepNetts</b><span>a deep-learning library for the JVM</span></span>
    <span class="badge">Zoran's stream</span>
  </div>
</div>

---

# How this works

<div class="grid three">
  <div class="card"><span class="n">TODO</span><span class="l">yours &mdash; the scaffold runs and fails exactly where you haven't filled it in</span></div>
  <div class="card"><span class="n">given</span><span class="l">run it as-is and watch</span></div>
  <div class="card"><span class="n">&#9733; BONUS</span><span class="l">for fast finishers, more wow, clearly marked</span></div>
</div>

<p class="note">Every exercise has a matching <strong>solution</strong>. No one gets left behind; no one gets bored.</p>

---

# Getting everything

<div class="grid two">
  <div class="card"><span class="n">JDK 26+</span><span class="l">any recent OpenJDK &mdash; verified on 27</span></div>
  <div class="card"><span class="n">JTaccuino</span><span class="l"><a href="https://jtaccuino.github.io">jtaccuino.github.io</a></span></div>
  <div class="card"><span class="n">the lab</span><span class="l"><code>git clone https://github.com/jtaccuino/devoxx-hol-2026</code></span></div>
  <div class="card"><span class="n">Maven deps</span><span class="l"><a href="https://github.com/jtaccuino/devoxx-hol-2026/releases">devoxx-hol-2026-m2.zip</a> &rarr; unpack into <code>~/.m2/repository</code></span></div>
</div>

<p class="note">The Maven bundle holds every artifact the notebooks resolve &mdash; including the <code>0.5-SNAPSHOT</code> builds &mdash; so the whole lab runs <strong>offline</strong>.</p>

---

# Ready?

Open `notebooks/exercises/01-jtaccuino-basics.ipynb`

and we begin.
