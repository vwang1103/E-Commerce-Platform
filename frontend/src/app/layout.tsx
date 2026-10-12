import type { Metadata, Viewport } from "next";
import { StorefrontShell } from "@/shared/components/StorefrontShell";
import { Toaster } from "@/shared/components/Toaster";
import "./globals.css";

export const metadata: Metadata = {
  title: { default: "Arcade", template: "%s | Arcade" },
  description: "Useful things for work, home, and everyday life.",
};

export const viewport: Viewport = {
  themeColor: "#16a34a",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" data-scroll-behavior="smooth">
      <body className="flex min-h-screen flex-col">
        <StorefrontShell>{children}</StorefrontShell>
        <Toaster />
      </body>
    </html>
  );
}
