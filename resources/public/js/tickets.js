var PAYMILL_PUBLIC_KEY = '8585298099281237d892403846aedaf0';

angular
    .module('ehsm', ['angularPayments', '$strap.directives'])
    .controller('TicketsController', ['$scope', function ($scope) {
        console.log('TicketsController');
        $scope.ticket = { donation: 0,
                          type: 'supporter' };
        $scope.totalAmount = 0;
        $scope.payment = {};
        $scope.fop = 'cc';
        $scope.earlyTicketAvailable = moment().isBefore('2014-02-02');
        $scope.tooltips = {
            studentTicket: "You will have to bring a valid student ID or unemployment confirmation to the conference",
            earlyTicket: "Available until February 1st, 2014",
            lateTicket: "After February 1st, 2014",
            supporterTicket: "We'll put your name onto the \"Supporters\" page on our web site",
            goldSupporterTicket: "We'll put your name and your logo onto the \"Supporters\" page on our web site and into our printed programme",
            participantName: "We're going to print this name onto your badge",
            participantName: "We're going to print this onto your badge, too",
            emailAddress: "We're going to send your payment confirmation and updates about EHSM 2014 to this email address",
            invoiceInformation: "If you want a name and address to appear on your invoice, please enter it here"
        }
        var ticketPrices = { student: 45,
                             regularEarly: 70,
                             regularLate: 95,
                             supporter: 272,
                             goldSupporter: 1337 };
        $scope.updateTotal = function () {
            console.log($scope.ticket);
            $scope.totalAmount = (parseInt($scope.ticket.donation) || 0) + ticketPrices[$scope.ticket.type];
        }
        $scope.updateTotal();
        $scope.doit = function () {
            console.log('submit', $scope);
            paymill.createToken({ number: $scope.payment.kontonummer,
                                  bank: $scope.payment.bankleitzahl,
                                  accountholder: $scope.payment.name },
                                function (error, result) {
                                    console.log('paymill error', error, 'result', result);
                                });
            return false;
        }
    }]);
