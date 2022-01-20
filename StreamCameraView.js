import {requireNativeComponent} from 'react-native';

export const COMPONENT_NAME = 'RCTNodeCamera';

const StreamCameraView = requireNativeComponent(COMPONENT_NAME);
module.exports = StreamCameraView;
