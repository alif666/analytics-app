import LogoutButton from "./LogoutButton";

  export default function AppBar() {
    return (
      <header className="flex h-16 items-center justify-between border-b border-border bg-surface px-6 text-foreground">
        <div>
          <div className="font-semibold">Analytics</div>
          <div className="text-xs text-muted">Spring Boot session auth</div>
        </div>
        <LogoutButton />
      </header>
    );
  }
