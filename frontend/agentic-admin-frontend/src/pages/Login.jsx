import { useState } from "react";
import { motion } from "framer-motion";
import { ShieldCheck } from "lucide-react";

export default function Login() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const handleLogin = () => {
    alert("Keycloak authentication will be integrated here");
  };

  return (
    <div className="min-h-screen relative overflow-hidden bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950 flex items-center justify-center text-white">

      {/* Subtle AI Background Animation */}
      <div className="absolute inset-0 -z-10 opacity-20">
        <svg className="w-full h-full">
          {[...Array(20)].map((_, i) => (
            <motion.circle
              key={i}
              cx={`${Math.random() * 100}%`}
              cy={`${Math.random() * 100}%`}
              r="2"
              fill="#6366f1"
              animate={{ y: [0, -15, 0] }}
              transition={{
                duration: 8 + Math.random() * 5,
                repeat: Infinity,
              }}
            />
          ))}
        </svg>
      </div>

      {/* Login Card */}
      <motion.div
        initial={{ opacity: 0, y: 40 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="w-full max-w-md bg-slate-900/80 backdrop-blur-xl rounded-3xl shadow-2xl p-10 border border-slate-800 relative"
      >
        {/* Subtle Glow */}
        <div className="absolute inset-0 bg-indigo-600 blur-3xl opacity-10 rounded-3xl"></div>

        <div className="relative">

          {/* Header */}
          <div className="flex items-center justify-center mb-6">
            <ShieldCheck className="text-indigo-500 mr-2" size={28} />
            <h2 className="text-3xl font-bold tracking-wide">
              Secure Administrative Login
            </h2>
          </div>

          <p className="text-center text-slate-400 text-sm mb-8">
            Authorized banking personnel access only
          </p>

          {/* Form */}
          <div className="space-y-5">
            <input
              type="text"
              placeholder="Username"
              className="w-full px-4 py-3 rounded-xl bg-slate-800 border border-slate-700 text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 transition"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />

            <input
              type="password"
              placeholder="Password"
              className="w-full px-4 py-3 rounded-xl bg-slate-800 border border-slate-700 text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 transition"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />

            <button
              onClick={handleLogin}
              className="w-full py-3 rounded-xl bg-indigo-600 text-white shadow-lg hover:bg-indigo-700 hover:shadow-xl transition font-bold tracking-wide"
            >
              Authenticate Securely
            </button>
          </div>

          {/* Security Notice */}
          <div className="mt-8 text-xs text-slate-500 text-center leading-relaxed border-t border-slate-800 pt-4">
            This is a restricted banking administrative system.
            All authentication attempts are monitored, recorded,
            and audited in accordance with financial regulatory
            compliance requirements.
            <br /><br />
            Unauthorized access is strictly prohibited and may result
            in legal action.
          </div>

          {/* Regulatory Footer */}
          <div className="mt-4 text-[11px] text-slate-600 text-center">
            Secure System • Licensed by Central Bank of Sri Lanka •
            Role-Governed Access Architecture
          </div>
        </div>
      </motion.div>
    </div>
  );
}
