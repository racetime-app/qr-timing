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
      <img class="hero__illustration" src="/static/img/qr-illustration.svg" width="240" height="240"
           alt="${msg("landing.illustration")}">
    </div>
  </section>

  <aside class="notice">
    <img class="notice__icon" src="/static/img/icon-link.svg" width="24" height="24" alt="">
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
