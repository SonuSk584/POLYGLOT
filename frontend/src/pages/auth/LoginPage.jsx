import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import PhoneInput from "react-phone-input-2";
import "react-phone-input-2/lib/style.css";
import OTPInput from "react-otp-input";

import {
  auth,
  RecaptchaVerifier,
  signInWithPhoneNumber,
  googleProvider,
} from "../../firebase";

import { useAuth } from "../../contexts/AuthContext";
import Button from "../../components/ui/Button";
import {signInWithPopup} from "firebase/auth";

const LoginPage = () => {
  const [phone, setPhone] = useState("");
  const [otp, setOtp] = useState("");
  const [step, setStep] = useState("phone");

  const [timer, setTimer] = useState(30);
  const [loading, setLoading] = useState(false);
  const [otpError, setOtpError] = useState(false);

  const navigate = useNavigate();
  const { firebaseLogin } = useAuth();

  // ⭐ Countdown Timer
  const startTimer = () => {
    setTimer(30);
    const interval = setInterval(() => {
      setTimer((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const generateRecaptcha = () => {
    window.recaptchaVerifier = new RecaptchaVerifier(auth, "recaptcha-container", {
      size: "invisible",
    });
  };

  const handleSendOtp = async (e) => {
    e.preventDefault();

    if (phone.length < 10) {
      alert("Enter a valid phone number");
      return;
    }

    try {
      setLoading(true);

      generateRecaptcha();
      const appVerifier = window.recaptchaVerifier;

      const fullPhone = "+" + phone;

      const confirmation = await signInWithPhoneNumber(
        auth,
        fullPhone,
        appVerifier
      );

      window.confirmationResult = confirmation;
      setStep("otp");
      startTimer();
    } catch (error) {
      console.log(error);
      alert("Failed to send OTP");
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const result = await window.confirmationResult.confirm(otp);
      const firebaseUser = result.user;

      const resp = await firebaseLogin(firebaseUser);

      if (resp.isNewUser || !resp.profileCompleted) {
        navigate("/profile/setup");
      } else {
        navigate("/chat");
      }
    } catch (error) {
      console.log(error);

      // Shake animation
      setOtpError(true);
      setTimeout(() => setOtpError(false), 500);

      alert("Invalid OTP");
    } finally {
      setLoading(false);
    }
  };
  const handleGoogleLogin = async () => {
  try {
    setLoading(true);
    const result = await signInWithPopup(auth, googleProvider);
    const resp = await firebaseLogin(result.user);

    if (resp.success) {
      if (resp.isNewUser || !resp.profileCompleted) {
        navigate("/profile/setup");
      } else {
        navigate("/chat");
      }
    }
  } catch (error) {
    console.log(error);
    alert("Google login failed");
  } finally {
    setLoading(false);
  }
};

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-50 to-secondary-50 dark:from-gray-900 dark:to-gray-800 p-4">
      <motion.div
        className="card max-w-md w-full p-6 rounded-2xl shadow-xl bg-white/90 dark:bg-gray-800/80 backdrop-blur-xl border border-gray-200 dark:border-gray-700"
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.35 }}
      >
        {/* App Title */}
        <h2 className="text-3xl font-bold text-center text-primary-600 dark:text-primary-300">
          Polyglot Chat
        </h2>
        <p className="text-center mt-2 text-gray-500 dark:text-gray-400">
          Chat without language barriers
        </p>

        <div id="recaptcha-container"></div>

        {/* ------------------------------------ */}
        {/* PHONE NUMBER SCREEN */}
        {/* ------------------------------------ */}
        {step === "phone" && (
          <motion.form
            onSubmit={handleSendOtp}
            className="mt-6 space-y-4"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
          >
            <label className="text-gray-700 dark:text-gray-200 font-medium">
              Phone Number
            </label>

            <PhoneInput
              country={"in"}
              value={phone}
              onChange={(val) => setPhone(val)}
              inputStyle={{
                width: "100%",
                height: "55px",
                fontSize: "16px",
                borderRadius: "12px",
              }}
              buttonStyle={{
                borderRadius: "12px 0 0 12px",
              }}
              containerStyle={{ width: "100%" }}
            />

            <Button type="submit" fullWidth isLoading={loading} className="mt-4">
              Send OTP
            </Button>
              {/* Divider */}
<div className="flex items-center gap-3 my-4">
  <div className="flex-1 h-px bg-gray-300 dark:bg-gray-600" />
  <span className="text-gray-400 text-sm">OR</span>
  <div className="flex-1 h-px bg-gray-300 dark:bg-gray-600" />
</div>

{/* Google Sign-In */}
<Button
  type="button"
  fullWidth
  variant="outline"
  onClick={handleGoogleLogin}
  isLoading={loading}
  className="flex items-center justify-center gap-2"
>
  <img
    src="https://www.svgrepo.com/show/475656/google-color.svg"
    alt="Google"
    className="w-5 h-5"
  />
  Sign in with Google
  </Button>
            
          </motion.form>
        )}

        {/* ------------------------------------ */}
        {/* OTP SCREEN (PREMIUM) */}
        {/* ------------------------------------ */}
        {step === "otp" && (
          <motion.form
            onSubmit={handleVerifyOtp}
            className="mt-8 text-center"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4 }}
          >
            <p className="mb-2 text-gray-600 dark:text-gray-300 text-lg">
              Enter the 6-digit verification code
            </p>

            <p className="font-semibold text-xl text-primary-600 dark:text-primary-400 mb-6 tracking-wide">
              +{phone}
            </p>

            {/* ⭐ OTP Boxes */}
            <motion.div
              className="flex justify-center gap-3 mb-6"
              animate={otpError ? { x: [-10, 10, -10, 10, 0] } : {}}
              transition={{ duration: 0.3 }}
            >
              <OTPInput
                value={otp}
                onChange={setOtp}
                numInputs={6}
                shouldAutoFocus
                inputType="tel"
                renderInput={(props, idx) => (
                  <motion.input
                    {...props}
                    key={idx}
                    whileFocus={{ scale: 1.12 }}
                    transition={{ type: "spring", stiffness: 300 }}
                    className="
                      w-12 h-14 md:w-14 md:h-16
                      text-center 
                      text-2xl font-semibold
                      text-gray-900 dark:text-white 
                      bg-white/60 dark:bg-gray-800/60 
                      backdrop-blur-xl
                      border border-gray-300 dark:border-gray-700 
                      rounded-2xl shadow-sm
                      focus:ring-2 focus:ring-primary-500 
                      transition-all
                    "
                  />
                )}
              />
            </motion.div>

            <Button
              type="submit"
              fullWidth
              className="mt-4 py-3 text-lg tracking-wide rounded-xl"
              isLoading={loading}
            >
              Verify OTP
            </Button>

            {/* Timer */}
            {timer > 0 ? (
              <p className="mt-4 text-gray-500 dark:text-gray-400">
                Resend OTP in <b>{timer}s</b>
              </p>
            ) : (
              <button
                className="mt-4 text-primary-600 dark:text-primary-400 font-medium underline"
                onClick={handleSendOtp}
                type="button"
              >
                Resend OTP
              </button>
            )}

            {/* Change Number */}
            <button
              className="mt-4 block text-sm text-gray-500 hover:text-gray-800 dark:hover:text-gray-300 underline"
              onClick={() => setStep("phone")}
              type="button"
            >
              Change phone number
            </button>
          </motion.form>
        )}
      </motion.div>
    </div>
  );
};

export default LoginPage;
