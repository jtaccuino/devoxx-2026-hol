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

<div class="pipeline">
  <div class="step"><b>JTaccuino</b>a Java REPL, in cells</div>
  <div class="arrow">&#8594;</div>
  <div class="step"><b>Hardwood</b>read the Parquet</div>
  <div class="arrow">&#8594;</div>
  <div class="step"><b>dflib</b>shape the data</div>
  <div class="arrow">&#8594;</div>
  <div class="step"><b>gog4j</b>draw it</div>
  <div class="arrow">&#8594;</div>
  <div class="step hl"><b>DeepNetts</b>predict it <em>(Zoran)</em></div>
</div>

<p class="note">One pipeline over the CelesTrak catalog &mdash; <strong>No Python. No context switch. Just the JVM.</strong></p>

---

# How this works

<div class="cards">
  <div class="card"><span class="n">TODO</span><span class="l">yours &mdash; the scaffold runs and fails exactly where you haven't filled it in</span></div>
  <div class="card"><span class="n">given</span><span class="l">run it as-is and watch</span></div>
  <div class="card"><span class="n">&#9733; BONUS</span><span class="l">for fast finishers, more wow, clearly marked</span></div>
</div>

<p class="note">Every exercise has a matching <strong>solution</strong>. No one gets left behind; no one gets bored.</p>

---

# Getting everything

<div class="cards">
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
