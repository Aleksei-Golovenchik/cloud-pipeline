/*
 * Copyright 2017-2021 EPAM Systems, Inc. (https://www.epam.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from 'react';
import {
  Button,
  Modal,
  Form,
  Input,
  Row,
  Spin,
  Tabs
} from 'antd';
import PropTypes from 'prop-types';
import PermissionsForm, {OBJECT_TYPES} from '../../../roleModel/PermissionsForm';

// eslint-disable-next-line
const NAME_VALIDATION_TEXT = 'Name can contain only letters, digits, spaces, \'_\', \'-\', \'@\' and \'.\'.';

const TABS = {
  info: 'info',
  permissions: 'permissions'
};

const capitalizeString = (string = '') => {
  return string.charAt(0).toUpperCase() + string.slice(1).toLowerCase();
};

@Form.create()
class EditItemForm extends React.Component {
  static propTypes = {
    onCancel: PropTypes.func,
    onSubmit: PropTypes.func,
    pending: PropTypes.bool,
    visible: PropTypes.bool,
    name: PropTypes.string,
    title: PropTypes.string,
    includeFileContentField: PropTypes.bool,
    storageId: PropTypes.string,
    item: PropTypes.object,
    tabs: PropTypes.arrayOf(PropTypes.string)
  };

  state = {
    activeTab: TABS.info
  }

  formItemLayout = {
    labelCol: {
      xs: {span: 24},
      sm: {span: 6}
    },
    wrapperCol: {
      xs: {span: 24},
      sm: {span: 18}
    }
  };

  handleSubmit = (e) => {
    const {item} = this.props;
    e.preventDefault();
    this.props.form.validateFieldsAndScroll((err, values) => {
      if (!err) {
        this.props.onSubmit(values, item);
      }
    });
  };

  onChangeTab = (key) => {
    this.setState({activeTab: key});
  };

  renderInfoForm = () => {
    const {getFieldDecorator} = this.props.form;
    const nameShouldNotBeTheSameValidator = (rule, value, callback) => {
      let error;
      if (this.props.name && value && value.toLowerCase() === this.props.name.toLowerCase()) {
        error = 'Name should not be the same';
      }
      callback(error);
    };
    return (
      <Form>
        <Form.Item {...this.formItemLayout} label="Name">
          {getFieldDecorator('name', {
            rules: [
              {
                required: true,
                message: 'Name is required'
              },
              {
                pattern: /^[\da-zA-Z._\-@ ]+$/,
                message: NAME_VALIDATION_TEXT
              },
              {validator: nameShouldNotBeTheSameValidator}
            ],
            initialValue: this.props.name
          })(
            <Input
              ref={this.initializeNameInput}
              onPressEnter={this.handleSubmit}
              disabled={this.props.pending} />
          )}
        </Form.Item>
        {
          this.props.includeFileContentField &&
          <Form.Item {...this.formItemLayout} label="Content">
            {getFieldDecorator('content')(
              <Input
                disabled={this.props.pending}
                type="textarea"
              />
            )}
          </Form.Item>
        }
      </Form>
    );
  };

  renderPermissionsForm = () => {
    const {storageId} = this.props;
    if (!storageId) {
      return null;
    }
    return (
      <PermissionsForm
        objectIdentifier={storageId}
        executeDisabled
        // todo: change objectType to dataStorageItem when item permissions API will be ready
        objectType={OBJECT_TYPES.dataStorage}
      />
    );
  };

  renderModalContent = () => {
    const {tabs} = this.props;
    const renderers = {
      info: this.renderInfoForm,
      permissions: this.renderPermissionsForm
    };
    if (tabs && tabs.length) {
      if (tabs && tabs.length === 1) {
        const renderFn = renderers[tabs[0]];
        return renderFn ? renderFn() : null;
      }
      return (
        <Tabs
          size="small"
          activeKey={this.state.activeTab}
          onChange={this.onChangeTab}
        >
          {tabs.map(tab => {
            const renderFn = renderers[tab];
            return renderFn
              ? (
                <Tabs.TabPane key={tab} tab={capitalizeString(tab)}>
                  {renderFn()}
                </Tabs.TabPane>
              ) : null;
          })}
        </Tabs>
      );
    }
    return this.renderInfoForm();
  };

  render () {
    const {activeTab} = this.state;
    const {resetFields} = this.props.form;
    const modalFooter = this.props.pending || activeTab === TABS.permissions
      ? false
      : (
        <Row>
          <Button
            onClick={this.props.onCancel}
          >
            Cancel
          </Button>
          <Button
            type="primary"
            htmlType="submit"
            onClick={this.handleSubmit}
          >
            OK
          </Button>
        </Row>
      );
    const onClose = () => {
      this.setState({activeTab: TABS.info});
      resetFields();
    };
    if (!this.props.visible) {
      return null;
    }
    return (
      <Modal
        maskClosable={!this.props.pending}
        afterClose={() => onClose()}
        closable={!this.props.pending}
        visible={this.props.visible}
        title={this.props.title}
        onCancel={this.props.onCancel}
        footer={modalFooter}
      >
        <Spin spinning={this.props.pending}>
          {this.renderModalContent()}
        </Spin>
      </Modal>
    );
  }

  initializeNameInput = (input) => {
    if (input && input.refs && input.refs.input) {
      this.nameInput = input.refs.input;
      this.nameInput.onfocus = function () {
        setTimeout(() => {
          this.selectionStart = (this.value || '').length;
          this.selectionEnd = (this.value || '').length;
        }, 0);
      };
    }
  };

  focusNameInput = () => {
    if (this.props.visible && this.nameInput) {
      setTimeout(() => {
        this.nameInput.focus();
      }, 0);
    }
  };

  componentDidUpdate (prevProps) {
    if (prevProps.visible !== this.props.visible) {
      this.focusNameInput();
    }
  }
}

export {TABS};
export default EditItemForm;
