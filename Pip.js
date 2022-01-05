import { NativeModules, DeviceEventEmitter } from "react-native";
import React from "react";

const enterPip = () => NativeModules.NodeMediaClient.enterPip();
const usePip = () => {
  const [isInPipMode, setIsInPipMode] = React.useState(false);

  React.useEffect(() => {
    const listener = DeviceEventEmitter.addListener("onPipChange", (e) =>
      setIsInPipMode(e === "in_pip")
    );

    return listener.remove;
  }, []);

  return { isInPipMode, enterPip };
};

module.exports = {
  enterPip,
  usePip,
};
