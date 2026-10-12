import Link from "next/link";

export default function NotFound() {
  return (
    <section className="container-page flex flex-col items-center py-28 text-center">
      <p className="eyebrow">404</p>
      <h1 className="page-title mt-3">We can&apos;t find that page</h1>
      <p className="mt-3 max-w-md text-slate-500">The link may be out of date, or the page may have moved.</p>
      <div className="mt-8 flex gap-3">
        <Link href="/" className="btn btn-primary">
          Back to the store
        </Link>
        <Link href="/shop/" className="btn btn-secondary">
          Browse products
        </Link>
      </div>
    </section>
  );
}
