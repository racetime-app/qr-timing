<#--
  Check-in form at /c/{eventId}/{checkpointId}/{key}. Design artboards "Check-in" and "Check-in, desktop".

  Model (CheckInController):
    info        CheckpointInfo: event name and checkpoint, from the RaceTime API
    form        CheckInForm: what the athlete typed so far (empty on first visit)
    formAction  where the form is sent to
  When the form was sent with mistakes, Spring's validation result for "form" is in the model too;
  layout.fieldError reads it.
-->
<#import "layout.ftlh" as layout>
<#assign msg = layout.msg>
<#assign checkpoint = info.checkpoint>
<#assign bibError = layout.fieldError("bib")>
<#assign pinError = layout.fieldError("pin")>
<@layout.page title=msg("checkin.title") width="wide" link="/" linkText=msg("header.what-is-this")>
<main class="container container--wide checkin">

  <#-- Where the athlete is: the event, and the checkpoint in a green card. -->
  <section class="checkin__place">
    <div class="event">
      <span class="eyebrow">${msg("checkin.event")}</span>
      <h1 class="event__name">${info.eventName}</h1>
    </div>

    <div class="place-card">
      <#if checkpoint.finish>
        <span class="eyebrow place-card__eyebrow">${msg("checkin.place.finish")}</span>
        <div class="place-card__finish">
          <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="#FFFFFF" stroke-width="2"
               stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M5 21V4"/>
            <path d="M5 4h12l-2 4 2 4H5"/>
          </svg>
          <span class="place-card__name">${msg("checkpoint.finish")}</span>
        </div>
      <#elseif checkpoint.name?has_content>
        <span class="eyebrow place-card__eyebrow">${msg("checkin.place.named")}</span>
        <span class="place-card__name">${checkpoint.name}</span>
      <#else>
        <span class="eyebrow place-card__eyebrow">${msg("checkin.place.numbered")}</span>
        <span class="place-card__name">${layout.checkpointLabel(checkpoint)}</span>
      </#if>
    </div>
  </section>

  <#--
    The form. Browsers check "required" and "pattern" before sending; the server checks the same rules again.
    The PIN field is always there: athletes without a PIN leave it empty, and the API decides whether one is needed.
  -->
  <form class="card checkin__form" method="post" action="${formAction}">
    <div class="field">
      <label class="field__label" for="bib">${msg("checkin.bib.label")}</label>
      <input class="field__input" id="bib" name="bib" type="text" inputmode="numeric" pattern="[0-9]*"
             maxlength="9" autocomplete="off" placeholder="214" required value="${form.bib!}"
             aria-describedby="bib-hint<#if bibError?has_content> bib-error</#if>"
             <#if bibError?has_content>aria-invalid="true"</#if>>
      <#if bibError?has_content><p class="field__error" id="bib-error">${bibError}</p></#if>
      <p class="field__hint" id="bib-hint">${msg("checkin.bib.hint")}</p>
    </div>

    <div class="field">
      <label class="field__label" for="pin">${msg("checkin.pin.label")}</label>
      <#-- The PIN is never sent back to the browser, so it has to be typed again after a problem. -->
      <input class="field__input field__input--pin" id="pin" name="pin" type="password" inputmode="numeric"
             pattern="[0-9]*" maxlength="12" autocomplete="off" placeholder="••••"
             aria-describedby="pin-hint<#if pinError?has_content> pin-error</#if>"
             <#if pinError?has_content>aria-invalid="true"</#if>>
      <#if pinError?has_content><p class="field__error" id="pin-error">${pinError}</p></#if>
      <p class="field__hint" id="pin-hint">${msg("checkin.pin.hint")}</p>
    </div>

    <button class="button" type="submit">${msg("checkin.submit")}</button>

    <p class="checkin__note">${msg("checkin.note")}</p>
  </form>

</main>
</@layout.page>
