"use client";

import { ChangeEvent, useEffect, useState } from "react";
import api from "@/lib/api";

type Monthly = { id?: number; month: number; year: number; totalUsage: number; usageUom: string; energyCharge: number; demandCharge: number; meterRent: number; vat: number; estimatedAmount: number; saved: boolean };
const money = (v: number) => `৳${v.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
const monthName = (month: number) => new Date(2000, month - 1, 1).toLocaleString(undefined, { month: "long" });

export default function MeterPage() {
  const [file, setFile] = useState<File | null>(null);
  const [load, setLoad] = useState("7");
  const [rows, setRows] = useState<Monthly[]>([]);
  const [saved, setSaved] = useState<Monthly[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => { api.get<Monthly[]>("/meter/monthly").then(r => setSaved(r.data)).catch(() => undefined); }, []);
  const onFile = (e: ChangeEvent<HTMLInputElement>) => { setFile(e.target.files?.[0] || null); setRows([]); setError(""); };
  const analyze = async () => {
    if (!file) { setError("Choose a monthly CSV file first."); return; }
    setBusy(true); setError("");
    try { const form = new FormData(); form.append("file", file); form.append("sanctionedLoad", load); const result = await api.post<Monthly[]>("/meter/analyze", form, { headers: { "Content-Type": "multipart/form-data" } }); setRows(result.data); }
    catch (e: unknown) { setError(message(e, "Analysis failed.")); } finally { setBusy(false); }
  };
  const save = async (row: Monthly) => {
    if (!file) return; setBusy(true); setError("");
    try { const form = new FormData(); form.append("file", file); form.append("month", String(row.month)); form.append("year", String(row.year)); form.append("sanctionedLoad", load); const result = await api.post<Monthly>("/meter/save", form, { headers: { "Content-Type": "multipart/form-data" } }); setRows(current => current.map(r => r.month === row.month && r.year === row.year ? result.data : r)); setSaved(current => [result.data, ...current]); }
    catch (e: unknown) { setError(message(e, "Could not save this month.")); } finally { setBusy(false); }
  };
  const alreadySaved = (row: Monthly) => saved.some(s => s.month === row.month && s.year === row.year);
  return <div className="mx-auto max-w-7xl space-y-8">
    <section className="meter-hero rounded-3xl p-7 text-white shadow-xl md:p-10"><p className="mb-3 text-xs font-bold uppercase tracking-[0.28em] text-teal-100">Meter intelligence</p><h1 className="text-3xl font-semibold tracking-tight md:text-5xl">Understand your monthly electricity cost.</h1><p className="mt-4 max-w-2xl text-sm leading-6 text-slate-200">Analyze a monthly export to calculate progressive LT-A residential charges. Analysis is temporary until you save an individual month.</p></section>
    <section className="grid gap-5 rounded-2xl border border-border bg-surface p-5 shadow-sm md:grid-cols-[1.5fr_1fr_auto] md:items-end"><label className="block text-sm font-semibold">Monthly CSV<input className="mt-2 block w-full cursor-pointer rounded-xl border border-dashed border-slate-400 bg-surface-2 p-3 text-sm" type="file" accept=".csv,text/csv" onChange={onFile}/><span className="mt-1 block text-xs font-normal text-muted">Required columns: Month, Year, Total Usage, Usage UOM</span></label><label className="block text-sm font-semibold">Sanctioned load (kW)<input className="mt-2 w-full rounded-xl border border-border bg-background px-3 py-3" type="number" min="0.01" step="0.01" value={load} onChange={e => setLoad(e.target.value)}/><span className="mt-1 block text-xs font-normal text-muted">Used for the demand charge</span></label><button onClick={analyze} disabled={busy} className="rounded-xl bg-accent px-5 py-3 font-semibold text-white disabled:opacity-60">{busy ? "Working..." : "Analyze"}</button></section>
    {error && <p className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</p>}
    {rows.length > 0 && <section className="rounded-2xl border border-border bg-surface p-5 shadow-sm"><h2 className="text-2xl font-semibold">Analysis result</h2><p className="mt-1 text-sm text-muted">Each row is calculated independently. Save only the months you want associated with your account.</p><div className="mt-5 overflow-x-auto"><table className="w-full min-w-[760px] text-left text-sm"><thead><tr className="border-b border-border text-muted"><th className="p-3">Month</th><th className="p-3">Usage</th><th className="p-3">Energy</th><th className="p-3">Demand</th><th className="p-3">VAT</th><th className="p-3">Estimated amount</th><th className="p-3">Save</th></tr></thead><tbody>{rows.map(row => <tr key={`${row.year}-${row.month}`} className="border-b border-border"><td className="p-3 font-medium">{monthName(row.month)} {row.year}</td><td className="p-3">{row.totalUsage.toLocaleString()} {row.usageUom}</td><td className="p-3">{money(row.energyCharge)}</td><td className="p-3">{money(row.demandCharge)}</td><td className="p-3">{money(row.vat)}</td><td className="p-3 font-semibold">{money(row.estimatedAmount)}</td><td className="p-3"><button disabled={busy || row.saved || alreadySaved(row)} onClick={() => save(row)} className="rounded-lg bg-slate-900 px-3 py-2 text-white disabled:cursor-not-allowed disabled:bg-slate-300">{row.saved || alreadySaved(row) ? "Saved" : "Save"}</button></td></tr>)}</tbody></table></div></section>}
    <section className="rounded-2xl border border-border bg-surface p-5 shadow-sm"><h2 className="text-2xl font-semibold">Saved monthly usage</h2>{saved.length === 0 ? <p className="mt-2 text-sm text-muted">No saved months yet.</p> : <div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-3">{saved.map(row => <div key={row.id} className="rounded-xl bg-surface-2 p-4"><p className="font-semibold">{monthName(row.month)} {row.year}</p><p className="mt-2 text-sm text-muted">{row.totalUsage.toLocaleString()} {row.usageUom}</p><p className="mt-1 text-lg font-semibold">{money(row.estimatedAmount)}</p></div>)}</div>}</section>
  </div>;
}

function message(error: unknown, fallback: string) { const e = error as { response?: { data?: { detail?: string; message?: string } }; message?: string }; return e.response?.data?.detail || e.response?.data?.message || e.message || fallback; }
