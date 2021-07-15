/*
 * Copyright 2017-2021 EPAM Systems, Inc. (https://www.epam.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import React from 'react';
import PropTypes from 'prop-types';
import {inject, observer} from 'mobx-react';
import {Alert, Icon, Popover} from 'antd';
import {Mutlizone} from '../../../utils/multizone';

@inject('multiZoneManager')
@observer
class MultizoneUrlPopover extends React.Component {
  state = {
    visible: false,
    fetched: false,
    fetching: false,
    error: undefined,
    multiZoneRequest: undefined
  };

  renderContent = () => {
    const {
      fetched,
      error,
      multiZoneRequest
    } = this.state;
    const {
      content
    } = this.props;
    if (!fetched) {
      return (
        <Icon type="loading" />
      );
    }
    if (error) {
      return (
        <Alert type="error" message={error} />
      );
    }
    if (
      !content ||
      typeof content !== 'function'
    ) {
      return null;
    }
    return content(multiZoneRequest);
  };

  fetch = () => {
    const {
      fetched,
      fetching
    } = this.state;
    if (!fetched && !fetching) {
      const {
        configuration,
        multiZone,
        multiZoneManager,
        runServiceUrlConfiguration,
        runId
      } = this.props;
      if (!configuration && !runServiceUrlConfiguration) {
        this.setState({
          fetched: true,
          fetching: false,
          error: 'URL configuration not provided'
        });
      } else {
        this.setState({
          fetching: true
        }, () => {
          let multiZoneRequest = multiZone;
          if (!multiZoneRequest && runId) {
            multiZoneRequest = multiZoneManager.getRunMultiZoneConfiguration(runId);
          }
          if (!multiZoneRequest) {
            multiZoneRequest = new Mutlizone(multiZoneManager.defaultRegion);
          }
          multiZoneRequest
            .check(configuration || {})
            .then(() => multiZoneRequest.checkRunServiceUrl(runServiceUrlConfiguration || {}))
            .then(() => {
              this.setState({
                fetching: false,
                fetched: true,
                multiZoneRequest
              });
            });
        });
      }
    }
  };

  handleVisibilityChange = (visible) => {
    this.setState({visible}, this.fetch);
  };

  render () {
    const {
      children,
      trigger,
      placement,
      content
    } = this.props;
    if (
      !content ||
      typeof content !== 'function'
    ) {
      return null;
    }
    return (
      <Popover
        trigger={trigger}
        content={this.renderContent()}
        onVisibleChange={this.handleVisibilityChange}
        placement={placement}
      >
        {children}
      </Popover>
    );
  }
}

MultizoneUrlPopover.propTypes = {
  children: PropTypes.node,
  content: PropTypes.func,
  trigger: PropTypes.arrayOf(
    PropTypes.oneOf(['hover', 'click'])
  ),
  placement: PropTypes.string,
  configuration: PropTypes.object,
  runServiceUrlConfiguration: PropTypes.object,
  runId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  multiZone: PropTypes.object
};

MultizoneUrlPopover.defaultProps = {
  trigger: ['hover']
};

export default MultizoneUrlPopover;
