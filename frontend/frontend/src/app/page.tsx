import Link from "next/link";

export default function Home() {
  return (
    <main className="flex flex-col items-center justify-center min-h-screen">
      <h1 className="text-4xl font-bold mb-6">Welcome to Crypto Portfolio</h1>

      <div className="flex gap-4">
        <Link href="/login" className="px-6 py-3 bg-blue-600 hover:bg-blue-700 rounded-lg">
          Login
        </Link>

        <Link href="/register" className="px-6 py-3 bg-gray-700 hover:bg-gray-800 rounded-lg">
          Register
        </Link>

        <Link href="/dashboard" className="px-6 py-3 bg-green-600 hover:bg-green-700 rounded-lg">
          Dashboard
        </Link>
      </div>
    </main>
  );
}
