<#--
  Confirmation after a check-in. Design artboard "Success".

  Model (CheckInController):
    result        CheckInResult: event, checkpoint and bib. result.repeated is true when the athlete had
                  already checked in here, and nothing new was recorded.
    recordedTime  the recorded time in the event's time zone, such as "14:32:08". Missing for a repeated
                  check-in whose first time RaceTime doesn't know.
-->
<#import "layout.ftlh" as layout>
<#assign msg = layout.msg>
<#assign checkpoint = result.checkpoint>
<#assign kind = checkpoint.finish?then("finish", "checkpoint") + result.repeated?then("-repeated", "")>
<@layout.page title=msg("success.title")>
<main class="container container--narrow status">

  <div class="status__icon status__icon--success">
    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#FFFFFF" stroke-width="3"
         stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M5 12.5l4.5 4.5L19 7.5"/>
    </svg>
  </div>

  <div class="status__heading">
    <h1 class="status__title">${msg("success.${kind}.heading")}</h1>
    <p class="status__text">${msg("success.${kind}.text")}</p>
  </div>

  <dl class="summary">
    <div class="summary__row">
      <dt>${msg("success.event")}</dt>
      <dd>${result.eventName}</dd>
    </div>
    <div class="summary__row">
      <dt>${msg("success.checkpoint")}</dt>
      <dd>${layout.checkpointLabel(checkpoint)}</dd>
    </div>
    <div class="summary__row">
      <dt>${msg("success.bib")}</dt>
      <dd class="mono">${result.bib}</dd>
    </div>
    <#if recordedTime??>
    <div class="summary__time">
      <dt>${msg("success.time")}</dt>
      <dd class="summary__clock">${recordedTime}</dd>
    </div>
    </#if>
  </dl>

  <p class="status__note">${msg("success.note")}</p>

</main>
</@layout.page>
