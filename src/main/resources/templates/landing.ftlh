<#--
  Landing page at /. Design artboards "Landing" (phone) and "Landing, desktop".
  Explains what QR Timing is; it has no functions of its own.

  Model: racetimeAppUrl, where the "RaceTime" links point to (LandingController).
-->
<#import "layout.ftlh" as layout>
<#assign msg = layout.msg>
<@layout.page title=msg("landing.title") width="landing" description=msg("landing.description")
    link=racetimeAppUrl linkText=msg("header.racetime") footer=msg("footer.landing")>
<main class="container container--landing landing">

  <section class="hero">
    <div class="hero__text">
      <span class="badge">${msg("landing.badge")}</span>
      <h1 class="hero__title">${msg("landing.headline")}</h1>
      <p class="hero__lead">${msg("landing.lead")}</p>
    </div>
    <div class="hero__art">
      <svg width="240" height="240" viewBox="0 0 240 240" role="img" aria-label="${msg("landing.illustration")}">
        <rect x="1" y="1" width="238" height="238" rx="28" fill="#FFFFFF" stroke="#D6E0D8" stroke-width="2"/>
        <rect x="28" y="28" width="62" height="62" rx="8" fill="#15803D"/>
        <rect x="38" y="38" width="42" height="42" rx="4" fill="#FFFFFF"/>
        <rect x="48" y="48" width="22" height="22" rx="3" fill="#15803D"/>
        <rect x="150" y="28" width="62" height="62" rx="8" fill="#15803D"/>
        <rect x="160" y="38" width="42" height="42" rx="4" fill="#FFFFFF"/>
        <rect x="170" y="48" width="22" height="22" rx="3" fill="#15803D"/>
        <rect x="28" y="150" width="62" height="62" rx="8" fill="#15803D"/>
        <rect x="38" y="160" width="42" height="42" rx="4" fill="#FFFFFF"/>
        <rect x="48" y="170" width="22" height="22" rx="3" fill="#15803D"/>
        <g fill="#10201A">
          <rect x="104" y="28" width="12" height="12" rx="2"/>
          <rect x="122" y="40" width="12" height="12" rx="2"/>
          <rect x="104" y="58" width="12" height="12" rx="2"/>
          <rect x="128" y="72" width="12" height="12" rx="2"/>
          <rect x="28" y="104" width="12" height="12" rx="2"/>
          <rect x="52" y="108" width="12" height="12" rx="2"/>
          <rect x="76" y="104" width="12" height="12" rx="2"/>
          <rect x="104" y="104" width="12" height="12" rx="2"/>
          <rect x="128" y="116" width="12" height="12" rx="2"/>
          <rect x="150" y="104" width="12" height="12" rx="2"/>
          <rect x="176" y="112" width="12" height="12" rx="2"/>
          <rect x="200" y="104" width="12" height="12" rx="2"/>
          <rect x="104" y="150" width="12" height="12" rx="2"/>
          <rect x="122" y="168" width="12" height="12" rx="2"/>
          <rect x="170" y="164" width="12" height="12" rx="2"/>
          <rect x="196" y="152" width="12" height="12" rx="2"/>
          <rect x="150" y="186" width="12" height="12" rx="2"/>
          <rect x="104" y="196" width="12" height="12" rx="2"/>
        </g>
        <rect x="150" y="150" width="12" height="12" rx="2" fill="#15803D"/>
        <rect x="188" y="188" width="24" height="24" rx="3" fill="#15803D"/>
      </svg>
    </div>
  </section>

  <aside class="notice">
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#0E5A2B" stroke-width="2"
         stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M10 13a5 5 0 0 0 7.07 0l3-3a5 5 0 0 0-7.07-7.07l-1.5 1.5"/>
      <path d="M14 11a5 5 0 0 0-7.07 0l-3 3a5 5 0 0 0 7.07 7.07l1.5-1.5"/>
    </svg>
    <p><strong>${msg("landing.notice.lead")}</strong> ${msg("landing.notice.text")}</p>
  </aside>

  <section class="section">
    <h2 class="section__title">${msg("landing.how.title")}</h2>
    <ol class="steps">
      <#list ["scan", "enter", "timed"] as step>
      <li class="step">
        <span class="step__number" aria-hidden="true">${step?counter}</span>
        <h3 class="step__title">${msg("landing.how.${step}.title")}</h3>
        <p class="step__text">${msg("landing.how.${step}.text")}</p>
      </li>
      </#list>
    </ol>
  </section>

  <section class="organisers">
    <div class="organisers__text">
      <span class="eyebrow organisers__eyebrow">${msg("landing.organisers.eyebrow")}</span>
      <h2 class="organisers__title">${msg("landing.organisers.title")}</h2>
      <p class="organisers__lead">${msg("landing.organisers.text")}</p>
    </div>
    <a class="button button--light" href="${racetimeAppUrl}">${msg("landing.organisers.button")}</a>
  </section>

</main>
</@layout.page>
