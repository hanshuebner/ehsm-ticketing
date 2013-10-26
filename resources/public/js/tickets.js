angular
    .module('ehsm', ['angularPayments', '$strap.directives'])
    .controller('TicketsController', ['$scope', function ($scope) {
        console.log('TicketsController');
        $scope.fop = 'cc';
        $scope.tooltips = {
            studentTicket: "You will have to bring a valid student ID or unemployment confirmation to the conference",
            supporterTicket: "We'll put your name onto the \"Supporters\" page on our web site",
            goldSupporterTicket: "We'll put your name and your logo onto the \"Supporters\" page on our web site and into our printed programme",
            participantName: "We're going to print this name onto your badge",
            emailAddress: "We're going to send your payment confirmation and updates about EHSM 2014 to this email address",
            invoiceInformation: "If you want a name and address to appear on your invoice, please enter it here"
        }
    }]);
