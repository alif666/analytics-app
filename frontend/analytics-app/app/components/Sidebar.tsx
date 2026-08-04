// components/Sidebar.tsx
  import Link from "next/link";

  export default function Sidebar() {
    return (
      <aside className="w-64 border-r border-border bg-surface p-4 text-foreground">
        <nav className="flex flex-col gap-2">
          <Link className="rounded px-3 py-2 text-foreground hover:bg-surface-2" href="/dashboard">
            Dashboard
          </Link>
          <Link className="rounded px-3 py-2 text-foreground hover:bg-surface-2" href="/meter">
            Meter
          </Link>
        </nav>
      </aside>
    );
  }
