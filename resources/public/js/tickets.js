angular
    .module('ehsm', [])
    .directive('formInputLine', function () {
        return {
            restrict: 'E',
            template: '<div class="control-group"><label class="control-label" for="{{name}}">{{label}}</label><div class="controls"><input type="text" id="{{name}}" placeholder="{{placeholder}}"/></div>',
            scope: true,
            link: function ($scope, element, attributes) {
                $scope.name = attributes.name;
                $scope.label = attributes.label;
                $scope.placeholder = attributes.placeholder;
            }
        }
    })
    .controller('TicketsController', ['$scope', function ($scope) {
        console.log('TicketsController');
    }]);
