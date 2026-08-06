"use client";

import { ChangeEvent, FormEvent, useState } from "react";
import api from "@/lib/api";

type Reading = { date: string; consumptionKwh: number; cost: number; overBudget: boolean };
type Analytics = {
  importId: number; filename: string; readingCount: number; startDate: string; endDate: string;
  totalKwh: number; averageDailyKwh: number; peakDailyKwh: number; peakDate: string;
  minimumDailyKwh: number; dailyBudget: number; ratePerKwh: number; recommendedDailyKwh: number;
  actualAverageDailyCost: number; projectedPeriodCost: number; budgetVariancePerDay: number;
  readings: Reading[];
};

const money = (value: number) => value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const kwh = (value: number) => value.toLocaleString(undefined, { maximumFractionDigits: 2 });

export default function MeterPage() {
  const [file, setFile] = useState<File | null>(null);
  const [dailyBudget, setDailyBudget] = useState("100");
  const [ratePerKwh, setRatePerKwh] = useState("12");
  const [analytics, setAnalytics] = useState<Analytics | null>(null);
  const [loading, setLoading] = useState(false);
  const [validating, setValidating] = useState(false);
  const [fileValid, setFileValid] = useState(false);
  const [error, setError] = useState("");

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!file) { setError("Choose a CSV file first."); return; }
    setLoading(true); setError("");
    try {
      const form = new FormData();
      form.append("file", file);
      form.append("dailyBudget", dailyBudget);
      form.append("ratePerKwh", ratePerKwh);
      const response = await api.post<Analytics>("/meter/upload", form, { headers: { "Content-Type": "multipart/form-data" } });
      setAnalytics(response.data);
    } catch (requestError: unknown) {
      const errorResponse = requestError as { response?: { data?: { detail?: string; message?: string } }; message?: string };
      setError(errorResponse.response?.data?.detail || errorResponse.response?.data?.message || errorResponse.message || "Upload failed.");
    } finally { setLoading(false); }
  };

  const onFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const selectedFile = event.target.files?.[0] || null;
    setFile(selectedFile); setAnalytics(null); setError(""); setFileValid(false);
    if (!selectedFile) return;
    setValidating(true);
    try {
      const form = new FormData(); form.append("file", selectedFile);
      await api.post("/meter/validate", form, { headers: { "Content-Type": "multipart/form-data" } });
      setFileValid(true);
    } catch (requestError: unknown) {
      const errorResponse = requestError as { response?: { data?: { message?: string; detail?: string } }; message?: string };
      setError(errorResponse.response?.data?.message || errorResponse.response?.data?.detail || errorResponse.message || "This file is not valid.");
    } finally { setValidating(false); }
  };
  const maximum = Math.max(...(analytics?.readings.map((row) => row.consumptionKwh) || [1]));
  const withinBudget = analytics && analytics.budgetVariancePerDay >= 0;

  return (
    <div className="mx-auto max-w-7xl space-y-8">
      <section className="meter-hero rounded-3xl p-7 text-white shadow-xl md:p-10">
        <div className="max-w-2xl">
          <p className="mb-3 text-xs font-bold uppercase tracking-[0.28em] text-teal-100">Meter intelligence</p>
          <h1 className="text-3xl font-semibold tracking-tight md:text-5xl">Turn usage into a daily plan.</h1>
          <p className="mt-4 max-w-xl text-sm leading-6 text-slate-200">Upload your meter export to see your real daily pattern, the kWh you can safely use each day, and exactly where cost is drifting above target.</p>
        </div>
      </section>

      <form onSubmit={submit} className="grid gap-5 rounded-2xl border border-border bg-surface p-5 shadow-sm md:grid-cols-[1.5fr_1fr_1fr_auto] md:items-end">
        <label className="block text-sm font-semibold">CSV export
          <input className="mt-2 block w-full cursor-pointer rounded-xl border border-dashed border-slate-400 bg-surface-2 p-3 text-sm" type="file" accept=".csv,text/csv" onChange={onFileChange} />
          <span className={`mt-1 block text-xs font-normal ${fileValid ? "text-emerald-600" : "text-muted"}`}>{validating ? "Checking format and data..." : fileValid ? "CSV format is valid" : "Checked immediately after selection · Date + usage columns required"}</span>
        </label>
        <label className="block text-sm font-semibold">Daily budget
          <input className="mt-2 w-full rounded-xl border border-border bg-background px-3 py-3 outline-none focus:border-accent" type="number" min="0.01" step="0.01" value={dailyBudget} onChange={(e) => setDailyBudget(e.target.value)} />
          <span className="mt-1 block text-xs font-normal text-muted">Your currency / day</span>
        </label>
        <label className="block text-sm font-semibold">Rate per kWh
          <input className="mt-2 w-full rounded-xl border border-border bg-background px-3 py-3 outline-none focus:border-accent" type="number" min="0.01" step="0.01" value={ratePerKwh} onChange={(e) => setRatePerKwh(e.target.value)} />
          <span className="mt-1 block text-xs font-normal text-muted">Needed to convert cost to usage</span>
        </label>
        <button disabled={loading || validating || !fileValid} className="rounded-xl bg-accent px-5 py-3 font-semibold text-white transition hover:brightness-110 disabled:cursor-wait disabled:opacity-60" type="submit">{validating ? "Checking file..." : loading ? "Analysing..." : "Analyse CSV"}</button>
      </form>
      {error && <p className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</p>}

      {analytics && <>
        <div className="flex flex-wrap items-end justify-between gap-3"><div><p className="text-xs font-bold uppercase tracking-[0.2em] text-accent">Latest analysis</p><h2 className="mt-1 text-2xl font-semibold">{analytics.filename}</h2></div><p className="text-sm text-muted">{analytics.startDate} to {analytics.endDate} · {analytics.readingCount} days</p></div>
        <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <Metric label="Recommended per day" value={`${kwh(analytics.recommendedDailyKwh)} kWh`} note={`Keeps cost at ${money(analytics.dailyBudget)} / day`} featured />
          <Metric label="Your average" value={`${kwh(analytics.averageDailyKwh)} kWh`} note={`${money(analytics.actualAverageDailyCost)} average daily cost`} />
          <Metric label="Total consumption" value={`${kwh(analytics.totalKwh)} kWh`} note={`${money(analytics.projectedPeriodCost)} across this period`} />
          <Metric label="Peak day" value={`${kwh(analytics.peakDailyKwh)} kWh`} note={analytics.peakDate} />
        </section>
        <section className="grid gap-5 xl:grid-cols-[1.5fr_1fr]">
          <div className="rounded-2xl border border-border bg-surface p-5 shadow-sm"><div className="mb-6 flex items-center justify-between"><div><h3 className="font-semibold">Daily usage</h3><p className="text-sm text-muted">Red bars exceed the daily budget</p></div><span className={`rounded-full px-3 py-1 text-xs font-bold ${withinBudget ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700"}`}>{withinBudget ? "On target" : "Needs attention"}</span></div><div className="space-y-3">{analytics.readings.map((row) => <div key={row.date} className="grid grid-cols-[72px_1fr_70px] items-center gap-3 text-sm"><span className="text-muted">{new Date(`${row.date}T00:00:00`).toLocaleDateString(undefined, { day: "2-digit", month: "short" })}</span><div className="h-7 overflow-hidden rounded-md bg-surface-2"><div className={`h-full rounded-md ${row.overBudget ? "bg-orange-400" : "bg-teal-500"}`} style={{ width: `${Math.max(3, (row.consumptionKwh / maximum) * 100)}%` }} /></div><span className="text-right font-medium">{kwh(row.consumptionKwh)}</span></div>)}</div></div>
          <div className="space-y-5"><div className="rounded-2xl bg-slate-900 p-6 text-white shadow-sm"><p className="text-xs font-bold uppercase tracking-[0.2em] text-teal-300">Your daily guardrail</p><p className="mt-4 text-4xl font-semibold">{kwh(analytics.recommendedDailyKwh)} <span className="text-base font-normal text-slate-300">kWh / day</span></p><p className="mt-3 text-sm leading-6 text-slate-300">At {money(analytics.ratePerKwh)} per kWh, staying under this level keeps you within your {money(analytics.dailyBudget)} daily target.</p></div><div className="rounded-2xl border border-border bg-surface p-6"><h3 className="font-semibold">What to do next</h3><p className="mt-3 text-sm leading-6 text-muted">{withinBudget ? `You are averaging ${money(Math.abs(analytics.budgetVariancePerDay))} under budget each day. Keep your usage below the guardrail.` : `You are averaging ${money(Math.abs(analytics.budgetVariancePerDay))} over budget each day. Shift heavy loads away from peak days and aim for the guardrail.`}</p><div className="mt-5 grid grid-cols-2 gap-3 text-sm"><div className="rounded-xl bg-surface-2 p-3"><span className="block text-xs text-muted">Lowest day</span><strong>{kwh(analytics.minimumDailyKwh)} kWh</strong></div><div className="rounded-xl bg-surface-2 p-3"><span className="block text-xs text-muted">Rate used</span><strong>{money(analytics.ratePerKwh)} / kWh</strong></div></div></div></div>
        </section>
      </>}
    </div>
  );
}

function Metric({ label, value, note, featured }: { label: string; value: string; note: string; featured?: boolean }) { return <div className={`rounded-2xl border p-5 shadow-sm ${featured ? "border-teal-200 bg-teal-50" : "border-border bg-surface"}`}><p className="text-xs font-bold uppercase tracking-[0.12em] text-muted">{label}</p><p className="mt-3 text-2xl font-semibold">{value}</p><p className="mt-2 text-xs text-muted">{note}</p></div>; }
