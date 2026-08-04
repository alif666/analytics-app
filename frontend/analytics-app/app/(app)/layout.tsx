// app/(app)/layout.tsx

import AppBar from "@/components/Appbar";
import AuthGate from "@/components/AuthGate";
import Sidebar from "@/components/Sidebar";

  export default function AppLayout({
    children,
  }: {
    children: React.ReactNode;
  }) {
    return (
      <AuthGate>
        <div className="min-h-screen bg-background text-foreground">
          <AppBar />
          <div className="flex min-h-[calc(100vh-64px)]">
            <Sidebar />
            <main className="flex-1 bg-background p-6 text-foreground">{children}</main>
          </div>
        </div>
      </AuthGate>
    );
  }
