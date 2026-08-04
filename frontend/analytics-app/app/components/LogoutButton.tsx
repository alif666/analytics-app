"use client";

import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";

export default function LogoutButton() {
  const router = useRouter();
  const { logout } = useAuth();

  const handleLogout = async () => {
    await logout();
    router.replace("/login");
  };

  return (
    <button
      type="button"
      onClick={handleLogout}
      className="rounded-full border border-border bg-surface px-3 py-1.5 text-sm text-foreground transition hover:bg-surface-2"
    >
      Logout
    </button>
  );
}
