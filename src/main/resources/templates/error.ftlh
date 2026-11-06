<#--
  Error page. Design artboards "Error: PIN", "Error: bib not found", "Error: outdated QR" and
  "Error: checkpoint skipped". Variants missing from the design (not started, too many attempts, API down,
  page not found) reuse the same layout.

  Model:
    screen    ErrorScreen: which variant to show. Its texts are error.<screen.key>.* in messages.properties.
    retryUrl  where "Try again" leads; only set for retryable screens (CheckInController)

  Spring Boot also renders this template for errors outside the check-in pages, see QrTimingErrorViewResolver.
-->
<#import "layout.ftlh" as layout>
<#assign msg = layout.msg>
<#assign key = screen.key>
<@layout.page title=msg("error.title")>
<main class="container container--narrow status">

  <div class="status__icon status__icon--error">
    <img src="/static/img/icon-alert.svg" width="44" height="44" alt="">
  </div>

  <div class="status__heading">
    <h1 class="status__title">${msg("error.${key}.heading")}</h1>
    <p class="status__text">${msg("error.${key}.text")}</p>
  </div>

  <#if (screen.retryable && retryUrl??) || screen.adviceShown>
  <div class="status__actions">
    <#if screen.retryable && retryUrl??>
    <a class="button" href="${retryUrl}">${msg("error.try-again")}</a>
    </#if>
    <#if screen.adviceShown>
    <p class="advice"><strong>${msg("error.what-to-do")}</strong> ${msg("error.${key}.advice")}</p>
    </#if>
  </div>
  </#if>

  <#if screen.retryable && retryUrl??>
  <p class="status__note">${msg("error.still-stuck")}</p>
  </#if>

  <a class="link-plain" href="/">${msg("error.what-is-qr-timing")}</a>

</main>
</@layout.page>
