import { NativeModules, DeviceEventEmitter } from "react-native";
import React from "react";

const setShouldEnterPip = (shouldEnter) => NativeModules.NodeMediaClient.setShouldEnterPip(!!shouldEnter);
const enterPip = () => NativeModules.NodeMediaClient.enterPip();
const usePip = () => {
  const [isInPipMode, setIsInPipMode] = React.useState(false);

  React.useEffect(() => {
    const listener = DeviceEventEmitter.addListener("onPipChange", (e) =>
      {
        console.log({e})
        setIsInPipMode(e === "in_pip")}
    );

    return () => { 
      if (listener.remove) listener.remove() 
    };
  }, []);

  return { isInPipMode, enterPip };
};

// pre disabling for avoid keep state on native side
setShouldEnterPip(false);

module.exports = {
  setShouldEnterPip,
  enterPip,
  usePip,
};
