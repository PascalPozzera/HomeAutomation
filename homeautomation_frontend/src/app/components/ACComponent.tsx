import { useState, useEffect } from "react";

export default function AirConditionComponent() {
    const [open, setOpen] = useState(false);
    const [isOn, setIsOn] = useState<boolean | null>(null);

    useEffect(() => {
        const interval = setInterval(fetchStatus, 2000);
        return () => clearInterval(interval);
    }, []);

    const fetchStatus = async () => {
        try {
            const res = await fetch("/api/ac/status");
            const state = await res.json();
            setIsOn(state);
        } catch (err) {
            console.error("Failed to fetch AC status:", err);
        }
    };

    const switchPower = async (on: boolean) => {
        try {
            await fetch(`/api/ac/power?on=${on}`, { method: "POST" });
            await fetchStatus();

        } catch (err) {
            console.error("Failed to switch AC power:", err);
        }
    };

    return (
        <>
            {/* AC Box */}
            <div
                onClick={() => setOpen(true)}
                className="relative bg-white shadow-md rounded-xl p-4 text-center cursor-pointer hover:shadow-lg transition"
            >
                {/* LED indicator */}
                <div className="absolute top-3 right-3">
                    {isOn !== null && (
                        <div
                            className={`w-3 h-3 rounded-full ${
                                isOn ? "bg-green-500" : "bg-red-500"
                            } border border-gray-300`}
                            title={isOn ? "On" : "Off"}
                        />
                    )}
                </div>

                <img src="/ac-icon.png" alt="AC" className="w-16 h-16 mx-auto mb-2" />
                <h2 className="text-lg font-semibold text-gray-800">AirCondition</h2>
                <p className="text-sm text-gray-500">Control air conditioning</p>
            </div>

            {/* Modal */}
            {open && (
                <div
                    className="fixed inset-0 backdrop-blur-sm bg-white/30 flex items-center justify-center z-50"
                    onClick={() => setOpen(false)}
                >
                    <div
                        className="bg-white rounded-lg p-6 w-96 shadow-xl"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="flex justify-between items-center mb-4">
                            <h3 className="text-xl font-bold text-gray-800">AC Control</h3>
                            <button
                                onClick={() => setOpen(false)}
                                className="text-gray-500 hover:text-black text-2xl"
                            >
                                &times;
                            </button>
                        </div>

                        <div className="text-center text-lg text-gray-700 mb-4">
                            Current status:{" "}
                            <span className={isOn ? "text-green-600 font-semibold" : "text-red-600 font-semibold"}>
                {isOn ? "ON" : "OFF"}
              </span>
                        </div>

                        <div className="space-y-2">
                            <div className="space-y-2">
                                <button
                                    onClick={() => switchPower(true)}
                                    className={`w-full py-2 rounded ${
                                        isOn
                                            ? "bg-blue-500 text-white"
                                            : "bg-gray-200 text-black hover:bg-gray-300"
                                    }`}
                                >
                                    Turn ON
                                </button>
                                <button
                                    onClick={() => switchPower(false)}
                                    className={`w-full py-2 rounded ${
                                        isOn === false
                                            ? "bg-blue-500 text-white"
                                            : "bg-gray-200 text-black hover:bg-gray-300"
                                    }`}
                                >
                                    Turn OFF
                                </button>
                            </div>

                        </div>
                    </div>
                </div>
            )}
        </>
    );
}
